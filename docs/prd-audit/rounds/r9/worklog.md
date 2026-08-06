# R9 锚点重构轮 工作台账

分支 docs/anchor-refactor-r9，基线 main=a5e90ed6。
边做边写，每完成一子步骤即追加。

## 开工基线（已核）
- 97a884d1... business-system-map-for-operations-v0.1.html  1881 行
- 6cf2e471... platform-governance-map-for-operations-v0.1.html  889 行
- 14ef83ca... business-enum-quick-reference-v0.1.html  256 行
- worktree ~/wt-r9 建立，HEAD=a5e90ed6

## T1 条目定名（完成）

生成器 `~/r9-scripts/gen-registry.js`（只读 HTML，零改动）。

定名规则（最终版）：
1. 条目粒度＝HTML 正文中的内容行；多行 `<tr>…</tr>` 合并为一条（表格一行＝一条）。
2. 排除：`<style>/<script>`、侧边目录 `<aside>` 导航、卡片装饰 `<span class="open-link">查看说明`、`<h1>`；`<h2>/<h3>` 不作为条目，仅作消歧用的章节上下文。
3. ENUM 及所有表格：取首个单元格（`<td class="k">` / 首个 td）文本为条目名。
4. 其余：先剥离通用前缀（已拍板／运营细则（已确认）／说明／注／提示／补充／备注 +「：」），再取首个全角冒号前文本；无冒号取首句（截至首个「。」），超 30 字截断。
5. 撞名消歧顺序：①「章节名·条目名」（h3 优先，无则 h2，再无则文件代号）→ ②「h2·h3·条目名」→ ③「文件代号·…」→ ④ 加 #序号。

产出 `docs/prd-audit/entry-registry.csv`：1075 条（BSM 658 / PGM 263 / ENUM 154）。
- 消歧 106 条（BSM 55 / PGM 18 / ENUM 33），其中需要加 #序号的 **0 条**。
- entry_name 全局唯一：1075 唯一名 / 1075 条，重名 0。
- 账本 829 个数字锚点 **100% 落入注册表条目区间**（miss 0，脚本 ~/r9-scripts/cov.js）。
- 注册表中尚无账本行的条目 249 条（R8 已知的「未登记 HTML 行」欠账，非本轮范围）。
- 人工抽验 20 条（BSM-E0004/0061/0141/0221/0301/0381/0461/0541/0621/0658、PGM-E0003/0043/0123/0203/0263、ENUM-E0010/0080/0110/0140/0154）：条目名与正文一一对应，无错配。

## T2 账本重建（完成）

修改器 `~/r9-scripts/rebuild-ledger.js`；独立校验器 `~/r9-scripts/verify-t2.js`（两套不同的 CSV 解析与比对实现）。

列结构：`anchor_or_line` 改造为 `entry_name`，并新增辅助列 `line_hint`（形如 L505，仅参考）。17 列 → 18 列，907 数据行不变。

统计：
- 映射到条目：829 行；ORPHANED（现行 HTML 已无原文，R8 留存的口径变更史）：78 行；UNMAPPED：0。
- source_text 刷新：**197 行改写**（冻结漂移一次性消解）、632 行原文未变。改前整行全量留档 `rounds/r9/ledger-source-refresh.csv`（197 行 + 表头）。
- subject/object 机械重抽：subject 246 格、object 250 格（含 NOT_EXTRACTED 槽位 67/71 格 + 原文已变行）。词表为闭合词表（subject 15 词、object 41 词），实测可复现性 347/352 = 98.6%。
- 句子类 9 列：原抽取规则**不可机械复现**（挖出高精度线索词后实测，对原判 NOT_STATED 的行误触发率 33%~54%），故不机械填充；其中因原文刷新导致引文已不在新原文中的 213 格（分布于 102 行）标为 `NOT_EXTRACTED_R9`，原值在 ledger-source-refresh.csv 全量可回溯。
- 保留的 `NOT_EXTRACTED_R8*` 格：592 格（原 730 格 − 已重抽 138 格）。

独立校验（`node ~/r9-scripts/verify-t2.js`，EXIT=0）：
```
列数 18 数据行 907 rule_id 重复 0
source_text 命中现行 HTML： 829 ｜ ORPHANED 跳过： 78 ｜ 不一致： 0
```

## T1 修订（并入 T2 之前）

按实际引用现状调整两点并回退重跑：
1. **条目名唯一性口径改为「同一份 HTML 内唯一」**（引用格式恒为 `文件代号「条目名」条`，文件+名即可全局唯一定位）。原按全局唯一会把 47 组「跨文件同名」条目强行改名（如 ENUM「权益类型」→「一、票务与权益·权益类型」），反而打断 PRD 里已有的正确引用。**此处与任务书「全局唯一」字面要求有偏差，理由与证据见收尾报告。**
2. 注册表同时登记**章节**（h2/h3，entry_kind=section）与**条目**（entry_kind=entry）：PRD 里大量引用形如 BSM「物业服务·主要功能」「员工手机端」，指向的就是章节。表头行 `<thead>` 不作为条目。

最终注册表：1210 行 = 条目 1059（BSM 645 / PGM 260 / ENUM 154）+ 章节 151（BSM 91 / PGM 47 / ENUM 13）；文件内消歧 53 条，需加 #序号者 0 条。

## T3 全库引用改写（机械部分完成）

修改器 `~/r9-scripts/rewrite-refs.js`（与校验方 `tools/verify-entry-anchors.mjs` 两套独立实现）。排除清单 35 份历史留痕文件（changelog / change-log / *-report / diff-matrix / anchor-rebuild / html-map / html-mirror-scan / history-index / reanchor / rounds 历史轮），纳入改写 341 份。

机械改写：**36 份文件、425 行**
| 规则 | 处数 |
|---|---|
| R1 删「锚点沿革／锚点更正」纯行号考古子句 | 33 子句 + 14 整括注 |
| R2 删「（YYYY-MM-DD 实测 LNNN）」「·实测 LNNN」等行号括注 | 328+120+12+5+2 = 467 |
| R3 `FILE LNNN「名」` → `FILE「名」条` | 145 |
| R4 `／LNNN「名」` → `／FILE「名」条` | 14 |
| R5 `「名」条 LNNN` → `「名」条` | 33 |
| R6 `「名」LNNN` → `「名」条` | 4 |

**`实测 L\d+` 在排除清单之外已归零（实测 0 处）。**
残留裸行号 198 行待逐处甄别（决策台账 103 + 其余 95），下一步逐处判语义。

### T3 逐处人工判语义（完成）

- 「实测 **LNNN**」加粗写法漏网 58 处（全部在决策台账），已补清（脚本 `~/r9-scripts/cleanup2.js`），门禁正则同步放宽到 `实测\s*\*{0,2}L\d+`。
- 定点改写 19 处 + 2 处（`manual-fix.js`/`manual-fix2.js`），每处断言 count===1：包括把 4 份文档里的 `BSM L1810` 改指 BSM「企业微信会话原文永久保存、不自动到期删除」条、修复 platform-governance §2A.4 七类红线被机械规则改坏的一行、merchant-points 两处账期依据改指两条「积分结算账期」条等。
- **条目名订正 58 处**（`namefix.js`，43 组，逐组人工判定目标条目）：例
  - ENUM「工单看板分行对照」→ ENUM「工单时效」（分行对照口径写在工单时效条内）
  - PGM「能耗运营细则（已确认）」→ PGM「表计按总表-分表分层、可总分表抹差核对」
  - BSM「赠送双轨」→ BSM「通票中的酒水、小食和其他权益必须分别绑定实际提供方」
  - BSM「物业费」（4 义）→ BSM「物业费按“每平米固定单价 × 商户计租面积”按自然月计费」
  - PGM「界面全中文（所有终端零英文）」（2 义）→ PGM「全局原则 · 界面全中文（所有终端零英文）」
  - BSM「排名档位」在决策台账里本就是「三处表述各不相同」的陈述句，改写为非锚点表述，不伪造成引用
- **编号订正**（依据决策台账 §七对照表，逐处判语义）：`settlement-rebuild-prd-v1.0.md` 6 处 + `ticketing-...-prd` 1 处
  | 位置 | 草案编号 | 正式编号 | 语义依据 |
  |---|---|---|---|
  | settlement §5.1 未核销权益 | DEC-R8-23/24/2D | **DEC-R8-21/22/31** | 场次终结制·语义甲 / 每次＝全新票权益 / 入场三道环节 |
  | settlement 减除的钱不进任何酒吧 | DEC-R8-21 | **DEC-R8-20** | 未核销减除额归属·三栏分列 |
  | settlement U-4 | DEC-R8-21 | **DEC-R8-20** | 同上 |
  | settlement U-6 | DEC-R8-26/27 | **DEC-R8-24/25** | 档位动态百分比制 / 档位挂覆盖集合内排名 |
  | settlement U-7 | DEC-R8-21 | **DEC-R8-20** | 三栏分列 |
  | settlement U-8 | DEC-R8-29 | **DEC-R8-27** | 域归属对照 |
  | ticketing 变更日志 | DEC-R8-2D | **DEC-R8-31** | 入场三道环节分开 |
  ENUM HTML L40 注释里的 DEC-R8-2D **未动**（属第三批草案 H-17 待批，且 HTML 本轮零改动）。
- **有意保留行号的位置**（属历史留痕，非现行锚点）：
  - 各 PRD 的「变更历史」表格行 71 行——记录的是当时做了什么，改写会篡改历史；
  - `stored-value-...-prd` §「锚点错位勘误表」5 行——该表本身就是记录旧行号错位的；
  - 决策台账正文里的历史行号 103 行——决策流水账，只加注不改原文（同 business-decision-log 处理原则）；
  - `pending-development-backlog` 与 `platform-governance-product-spec` 各 1 处 LNNN 指的是**别的 .md 文件的行号**，与三份 HTML 无关。

## T4 机检门禁（完成）

新增 `tools/verify-entry-anchors.mjs`（校验方，与修改方 `~/r9-scripts/*` 两套独立实现），`package.json` 挂 `verify:entry-anchors` 并接入 `verify:predeploy` 链首段。**不依赖 git diff 基线，任何分支任何时候跑都是全量实检，不存在空转假通过。**

检三件事：①注册表条目名在同一份 HTML 内唯一 + 每条首句指纹能在 HTML 现文命中；②全库 Markdown（历史留痕除外）的 `文件代号「名」` 引用能在该 HTML 内唯一定位；③正文不得再出现「实测 LNNN」（含加粗写法）。

正向：
```
[entry-anchors] 注册表 1210 行；被检 Markdown 341 份；条目名引用 819 处（注册表原名 722 处，正文唯一定位 97 处）。
[entry-anchors] PASS：条目名唯一、引用可唯一定位、正文无行号锚点。
EXIT=0
```

反向测试 A（把 building-operations-prd L17 的 ENUM「工单时效」改回旧称谓）：
```
[entry-anchors] FAIL：1 处不合格。
  - docs/building-operations-prd-v1.0.md:17：ENUM「工单看板分行对照」在该 HTML 中定位不到任何条目
EXIT_A=1
恢复后 → PASS，EXIT_A_RESTORED=0
```

反向测试 B（在同一行写回行号锚点「（2026-08-06 实测 L215）」）：
```
[entry-anchors] FAIL：1 处不合格。
  - docs/building-operations-prd-v1.0.md：仍有行号锚点 实测 L215（共 1 处）
EXIT_B=1
恢复后 → PASS，EXIT_B_RESTORED=0
```

反向测试 C（改坏注册表第 6 行的首句指纹，模拟「HTML 改了、注册表没重建」）：
```
[entry-anchors] FAIL：1 处不合格。
  - 注册表指纹在 HTML 现文中找不到：BSM-E0005 BSM「综合体里不是只有一个业务」——HTML 已改而注册表未重建
EXIT_C=1
恢复后 → PASS，EXIT_C_RESTORED=0
```
三次反向测试后 `git diff --stat` 确认被改文件已完全还原。

## T8 非代码遗留清零

### ① REQ-17 红灯——根因判定：**登记侧过期，不是代码缺陷**

- 红灯原文（`npm run verify:partial-requirement-mock-coverage`）：
  ```
  PARTIAL_REQUIREMENT_MOCK_COVERAGE_FAIL
  - REQ-17 apps/web/src/admin/pages/BarCommissionsPage.tsx must include marker 15%
  - REQ-17 apps/web/src/admin/pages/BarCommissionsPage.tsx must include marker 10%
  ```
- 判定逻辑：`tools/generate-partial-requirement-mock-coverage.mjs` 第 15 行把 REQ-17 的 admin_ui 证据登记为「该页面必须含字面量 `15%` 与 `10%`」。
- 证据链（只读核对 `apps/web/src/admin/pages/BarCommissionsPage.tsx` 与 git 历史）：
  - 提成费率于 **2026-07-28 commit `6513ef4b`（V349，“G 卡座销售提成改按员工岗位判定 + 修页面费率与台账对不上”）** 被**有意从前端删除**：该提交把 `commissionRate()` 的 `return isSalesOrder(order) ? "15%" : "10%"`、KPI 卡片的 `15% · N 单`／`10% · N 单`、文案里的「销售人员 15%／非销售人员 10%」全部删掉，改为读后端可配费率（`commissionRateLabel()`／`barSalesRole.ts`），提交说明明确写明真源口径是「**费率后台可配（出厂默认销售 15%、非销售 10%）**」。
  - 现页面只剩 `data-testid="bar-commissions-page"`、`bar-commissions-kpi-sales`、`bar-commissions-kpi-non-sales` 等稳定标记，文案为「按销售人员费率／按非销售人员费率」。
  - 也就是说：**门禁在要求页面重新硬编码 15%／10%，而这正是业务 HTML「业务策略一律可配置」原则与 V349 有意消除的东西。** 继续按门禁改 `apps/` 等于把已修复的硬编码退回去。
- 处置：改**登记侧**——把 REQ-17 该行的 marker 从 `["bar-commissions-page","15%","10%"]` 改为 `["bar-commissions-page","bar-commissions-kpi-sales","bar-commissions-kpi-non-sales"]`，并按工具自身规则重生成它管辖的两份产物（`docs/partial-requirement-mock-coverage-latest.json` / `-v0.1.md`）。
- 结果：`npm run verify:partial-requirement-mock-coverage` → `PARTIAL_REQUIREMENT_MOCK_COVERAGE_PASS`，EXIT=0。**apps/ 与 backend/ 零改动。**
- ⚠ **范围偏差**：批准计划 §4 写「tools 仅允许新增 verify-entry-anchors.mjs 与 package.json 挂链一行」，本处动了 `tools/generate-partial-requirement-mock-coverage.mjs` **一行 marker**。依据是 T8① 明文授权「若属登记侧过期可修则修」，且登记侧就在该工具里（只改文档产物会被下次重生成冲掉）。请终审确认此项。

### ② R8-U-06（决策日志 §8 三维容量口径）

`docs/business-decision-log-v0.1.md` §8「游乐容量」整节前加【已作废（D-02／DEC-R8-04，2026-08-05 裁决，2026-08-06 落 HTML）】标注，写明现行口径为「只按单个游乐项目的实时在玩人数控制容量，场次不参与容量占用」，并点名现行落点条目；**原文三条一字未删**（决策日志是历史流水，只加注不删）。findings 状态 → `fixed-in-r9`。

### ③ R8-U-07（REQ-110 公区能源费）

`docs/requirement-traceability-matrix-v0.1.md` REQ-110：
- 需求描述「公区能源费分摊并写 AR」→「商户能源费按各自表计实际用量与实际费用代收并写 AR，公区能源费统一含在物业费中、不单独分摊、不另行生成账单（D-01／DEC-R8-03／DEC-R8-11）」；
- 引用「业务 HTML §十……公区能源费分摊」→ 去掉已作废条目名，改引 ENUM「能源费与损耗费」条；
- 覆盖状态「已覆盖」→「**部分覆盖**」，并写明「合同 `energy_fee_basis` 的分摊取值与 `POST /settlements/energy-fee` 的公区分摊分支须按 D-01 收敛，属**代码轮**待办」。
findings 状态 → `fixed-in-r9`。

### ④ findings.csv 全部 open 项逐条甄别（改前 46 条 → 改后 41 条）

**本轮处理掉 5 条（状态 → `fixed-in-r9`）**

| finding | 甄别结论 | 处置 |
|---|---|---|
| R8-U-01 账本正文与 HTML 不一致（259 行） | 非代码，本轮 T2 正是干这件事 | source_text 全量刷新 197 行；829 行逐行回比一致（不一致 0）；78 行 ORPHANED 按 R8 决定保留为口径变更史 |
| GAP-35 CSV 台账正文残余陈旧 | 同上，与 R8-U-01 同一件事 | 随 T2 关闭 |
| R8-U-05 layer-spec 并列措辞「场次、容量」 | 非代码可修 | L81／L163 改为「场次编排、项目容量（实时在玩人数）」 |
| R8-U-06 决策日志三维容量 | 非代码可修 | 见 ② |
| R8-U-07 REQ-110 公区能源费 | 非代码可修 | 见 ③ |

**仍 open 41 条，逐条甄别结论**

| finding | 甄别结论 |
|---|---|
| GAP-01 营业日配置全面不符（钱） | **代码轮**：`business_day_cutoff_config` 按 domain 维、无闭店时点，与 HTML 10:00 起算口径不符，改的是后端与迁移 |
| GAP-02 差异表状态词表不符 | **代码轮**：对账差异表枚举取值 |
| GAP-03 超池仍为结算期出口 | **代码轮**：结算引擎分支 |
| GAP-04 池分配未逐票减除（钱） | **代码轮**：settlement 引擎由月度汇总改逐票，DEC-R8-20/21 落地 |
| GAP-05 恒等式改造顺序依赖 | **代码轮**：`verifyConservation` 改造顺序 |
| GAP-06 收入流需扩第 10 类（钱） | **代码轮**：`revenue_stream_attribution` 扩枚举＋迁移 |
| GAP-07 高价值阈值五处不符 | **代码轮**：阈值取数改读配置 |
| GAP-08 多股东模型缺失 | **代码轮**：分红引擎仅两方 |
| GAP-09 发放保护线只进不出（钱） | **代码轮**：留存释放机制未实现 |
| GAP-10 分红收入取数口径过时 | **代码轮** |
| GAP-11 库存流水 E-26(quantity>0) | **代码轮**：DB 约束 |
| GAP-12 报损与仓库管控无实现 | **代码轮** |
| GAP-13 服务费与小费无账单列 | **代码轮**：账单模型 |
| GAP-14 最晚到店时间零承载 | **代码轮** |
| GAP-15 转存能力零承载 | **代码轮** |
| GAP-16 转让模型不一致 | **代码轮** |
| GAP-17 账单三态未落地 | **代码轮** |
| GAP-18 桌台生命周期无独立列 | **代码轮**：迁移 |
| GAP-19 手机号无条件唯一 | **代码轮**：约束与注销复注册口径（DEC-R6-11） |
| GAP-20 跨项目下钻未走审批 | **代码轮**：权限闸 |
| GAP-21 页面权限字段矩阵未同步 | **代码轮（文档随码同批）**：矩阵内容是页面 ID／权限码的镜像，脱离代码单改文档只会制造新漂移 |
| GAP-22 招商域页面 ID 前缀不一致 | **代码轮（同上）** |
| GAP-23 UI 验收矩阵缺六行 | **代码轮（同上）** |
| GAP-24 代码/迁移/OpenAPI 待改造 | **代码轮**（本条自述即为代码） |
| GAP-26 开关引擎 opt-out→opt-in | **代码轮** |
| GAP-27 游乐场次 7 态持久化 | **代码轮**：DB 现 4 态 |
| GAP-28 商户服务工单分表独立七态 | **代码轮** |
| GAP-29 订台定金达标原路退回 | **代码轮** |
| GAP-30 退品低消保护判定缺失 | **代码轮** |
| GAP-31 核销「预检通过」态移除 | **代码轮**：枚举与迁移 |
| GAP-32 游乐「当天」营业日修正 | **代码轮**：`verifyUnlimitedIn...` 判日逻辑 |
| GAP-33 异常预警页面零承载 | **代码轮**：前端页面 |
| GAP-34 拆台/撤销转台/循环上限 | **代码轮** |
| GAP-37 收款/收入主体拆两列后实现仍单列 | **代码轮**：迁移＋引擎 |
| GAP-38 库存流水白名单缺三个 code | **代码轮**（文档两层已补齐，缺的是代码白名单） |
| GAP-39 取消取票后仍残留「取票」能力 | **代码轮**：前端/接口残留 |
| GAP-40 风控/对标阈值仍为旧自设值 | **代码轮**：阈值改可配 |
| GAP-41 分红前置借款清偿实现无落点 | **代码轮** |
| R8-U-02 HTML 内容无账本落点 | **非代码，但超本轮授权范围**：R8 计 161 行；R9 注册表口径下实测为 **233 条条目无账本行**。批准计划 T2 只授权「重建 908 行」，不含扩登记（会改变账本规模与 coverage 对应关系），留**下一轮账本扩登记**。 |
| R8-U-03 coverage.csv 9 行伪 rule_id | **非代码，但超本轮授权范围**：`docs/prd-audit/html-prd-coverage.csv` 不在计划授权改动清单内；且 9 行该挂到哪条规则需人工判定，属**账本/矩阵轮**。 |
| R8-U-04 45 条 PARTIAL 未逐句核验 | **非代码，但需人工逐句核验**：机检只证明 PRD 里有片段、不证明口径一致，属**承接核验轮**，非本轮锚点重构范围。 |

> 另：`docs/prd-audit/html-prd-coverage.csv` 的 `html_anchor_or_line` 列本轮**未动**，仍是行号锚点。计划 T2 只点名 `authority-rule-ledger.csv`，故未扩围；这是本轮结束后**唯一还在用行号锚的机读台账**，建议下一轮同法改造（已列入剩余风险）。

## 收工核验

### 三份 HTML 零改动（开工＝收工）

| 文件 | 开工 sha256 | 收工 sha256 | 行数 |
|---|---|---|---|
| business-system-map-for-operations-v0.1.html | `97a884d1…` | `97a884d1ce21ff3f7f5e7e0c857de65a24624a7ef0e65c940eddf16ffb505c47` | 1881 |
| platform-governance-map-for-operations-v0.1.html | `6cf2e471…` | `6cf2e4718e68d79980d907c9aee14c431f9d0928ea78ee23d400288f1cdb527c` | 889 |
| business-enum-quick-reference-v0.1.html | `14ef83ca…` | `14ef83ca0c80d475f7fedc1ff0d9c1d4c6df3714f962373377b032e418a21243` | 256 |

`git status` 中无任何 `.html`、`apps/`、`backend/`、迁移、`docs/openapi/` 改动。

### 行尾事故与修复（自查发现，已修）

python 写文件时用了 `newline='\n'`，把 4 份原本 CRLF（其中决策台账为 **1447 CRLF + 76 LF 混合**）的文件整体改了行尾，制造出上千行纯行尾噪声 diff。已逐份还原：
- `business-decision-log-v0.1.md` / `business-operations-layer-spec-v0.2.md` / `authority-rule-ledger.csv` / `entry-registry.csv` / `ledger-source-refresh.csv` / `findings.csv` → 统一 CRLF（与各自 HEAD 一致）；
- `business-decision-ledger.md` → 按 HEAD **逐行**还原混合行尾（脚本 `~/r9-scripts/restore-eol.py`），diff 由 232 行降到 **56 行真实内容改动**。
复核：所有改动文件的「LF 行数」与 HEAD 一致（package.json 因新增一行脚本 +1，属预期）。

### 标点残渣自查（自查发现，已修）

机械删子句会留下 `（，` `，）` 之类断句。全库扫 `（，|，）|（；|；）|（）|「」` → 发现 2 处（`service-order-product-prd` 抬头、决策台账 DEC-R7-38），已改写为完整句；复扫为 **0**。
另扫全角括号配对与重复标点：新增行中无不配对（8 处不配对为原文跨行排版，非本轮引入），重复标点 0。

### 命令与结果

| 命令 | 结果 |
|---|---|
| `node ~/r9-scripts/verify-t2.js` | EXIT=0；18 列 907 行、rule_id 重复 0、source_text 命中 829、ORPHANED 78、**不一致 0** |
| `node tools/verify-entry-anchors.mjs` | EXIT=0；注册表 1210 行、被检 md 341 份、条目名引用 823 处（原名 726 + 正文唯一定位 97）、**PASS** |
| 反向测试 A/B/C | 三次 EXIT=1，恢复后 EXIT=0（输出见 T4 节） |
| `npm run verify:predeploy` | **EXIT=0**，14 个子门禁全 PASS（含新挂的 entry-anchors） |
| `npm run verify:partial-requirement-mock-coverage` | EXIT=0，`PARTIAL_REQUIREMENT_MOCK_COVERAGE_PASS`（REQ-17 红灯已灭） |
| `npm run verify:external-evidence-requirement-traceability` | EXIT=0 |
| `npm run verify:integration-capability-switches` | EXIT=0 |
| `verify:closed-loop-current-state` / `verify:final-closed-loop-audit` / `verify:production-closure-gap-reports` | EXIT=1，**但在干净的 main 上同样 EXIT=1、报错逐字相同 → 属既有基线红，非本轮引入**；三者不在 predeploy 链内 |

### 改动规模

47 个已跟踪文件改动（1403 插入 / 1400 删除）＋ 3 个新增：`docs/prd-audit/entry-registry.csv`、`docs/prd-audit/rounds/r9/`（worklog + ledger-source-refresh.csv）、`tools/verify-entry-anchors.mjs`。

---

# 增补一：html-prd-coverage.csv 条目名化

## ⚠ 交办前提与实际不符（先纠正，再执行）

交办写「`html_anchor_or_line` 列按注册表转条目名」。**该列在这份文件里不存在。** 实测表头：

```
rule_id,prd_path,prd_anchor_or_line,coverage_status,severity,finding_id,conflict_summary,recommended_change,decision_id,phase
```

`prd_anchor_or_line` 装的是 **PRD 侧**锚点（如「§2-§14 产品总纲与已确认规则」「全文/综合条款」），不是 HTML 行号；全列 910 行里只有 2 个 `LNNN`，指的还是 `service-order-product-prd-v0.1.md` 自己的第 3 行。**所以这份文件本来就没有 HTML 行号可转** —— 它对 HTML 侧只靠 `rule_id` 关联。

按交办给的第二个选项（「新增 entry_name 列……与 authority-rule-ledger 做法一致」）执行。

## 做法与统计

- 在 `rule_id` 之后插入 **`entry_name`** 列（11 列），取值来自账本同 `rule_id` 的 `entry_name`。
- 统计：**910 行** = 映射到条目 **823** ／ ORPHANED **78**（账本里本就是 ORPHANED 的行，留原值）／ **NOT_IN_LEDGER 9**（即 R8-U-03 登记的 `*-MPS-000N` 伪 rule_id，账本中无对应，如实标出而不臆造）。
- **不做人工填写**：这一列是从账本抄来的，抄错就是新的漂移源，所以同批把门禁加严——见下。

## 门禁扩围（检查 ④）

`tools/verify-entry-anchors.mjs` 新增：
- 账本每行 `entry_name` 必须在注册表里存在（ORPHANED 除外）；
- 承接矩阵每行 `entry_name` 必须与账本同 `rule_id` **逐字相同**；标 `NOT_IN_LEDGER` 的行反过来必须确实不在账本里。

**反向测试 D**（把矩阵第一行条目名改成「综合体里不是只有一个业务XX」）：
```
[entry-anchors] FAIL：1 处不合格。
  - 承接矩阵与账本条目名不一致：BSM-0001 矩阵「综合体里不是只有一个业务XX」≠ 账本「综合体里不是只有一个业务」
EXIT_D=1
恢复后 → PASS，EXIT_D_RESTORED=0
```

---

# T9 第三批 HTML 落地（H-15~H-20，9 个落点）

落笔依据：`D:\AI\ccwork\prd-audit-r8\HTML修改建议草案-第三批.md` **§F 终稿文本**（全文含 v1.2 终批状态表已通读；本批无待批项）。
脚本 `~/r9-scripts/t9-html.py`：全部**行内替换**，逐处 `count == 1` 断言 ＋ 行数不变断言，任一处对不上即整体不落盘（先跑无 `--write` 干跑通过后才落）。

## 逐落点前后对照

| # | 文件·条目 | 改前（节选） | 改后（节选） | 依据 |
|---|---|---|---|---|
| 1 | ENUM「酒吧通票可分配池」总述＋取表＋排名句 | 「就由那几家酒吧按其在**本张票覆盖集合内**的排名分……按本张票**覆盖的酒吧家数**查「覆盖家数档位表」……**每张票每家酒吧能分到多少，在售票时即可算定。**排名一律在本张票的**覆盖集合内部**编列」 | 「由该票覆盖的酒吧中**参加第二段的那几家**，按其在**第二段参与者内部**的排名分配——**已从第一段取得份额的酒吧不占第二段名次，其后名次依次前移**……按本张票的**第二段参与家数**查「**参与家数档位表**」（原称「覆盖家数档位表」，同一张表两个叫法）……**并列均分整段**（含算例 17.5%、适用比例不取整、尾差单独留痕）」；**承诺句整句删除** | H-15＋H-18＋H-20 |
| 2 | ENUM 同条·超表句 | 「**覆盖家数**超出表内最大家数的……」 | 「**第二段参与家数**超出表内最大家数的……」 | H-18 |
| 3 | ENUM 同条·换行句 | 「**覆盖家数**决定取哪一行，与是否核销无关：……」 | 「**第二段参与家数**决定取哪一行，与是否核销无关：……」 | H-18 |
| 4 | ENUM 同条·档位表 | 「**覆盖家数档位表**（出厂默认…）：**覆盖 1 家**＝第 1 名 100%；**覆盖 2 家**＝…」 | 「**参与家数档位表**（出厂默认…）：**第二段参与 1 家**＝第 1 名 100%；**第二段参与 2 家**＝…」 | H-18；**数值一个未动**——改后文本由原文机械派生（只替换表名与 10 个行前缀），杜绝抄错数字 |
| 5 | ENUM「主要收入归属」 | 「计费基数为逐票按该票**覆盖集合**的档位表所对应的百分比份额」 | 「……按该票**第二段参与集合**的档位表……」 | H-18 连带 |
| 6 | ENUM 入场三步流程行上方 HTML 注释 | 「2026-08-06 **DEC-R8-2D** 重申……」 | 「2026-08-06 **DEC-R8-31** 重申……」 | H-17 |
| 7 | BSM「酒吧通票可分配池的池金额……」 | 「按其在**本张票覆盖集合内**的排名……查……的**覆盖家数档位表**取对应一行，**每张票每家酒吧分到多少在售票时即可算定，**排名在**覆盖集合内部**编列」 | 「由该票覆盖的酒吧中**参加第二段的那几家**按其在**第二段参与者内部**的排名分配（**拿了第一段的不占名次，其后名次前移**）……查……的**参与家数档位表**……**并列均分摘要句**」；**承诺句整句删除**；下一句「覆盖家数决定取哪一行」→「**第二段参与家数**决定取哪一行」 | H-15＋H-18＋H-20 |
| 8 | BSM「通票未核销减除额」 | 「按该票**覆盖集合**的档位表百分比从可分配池中减除」 | 「按该票**第二段参与集合**的档位表百分比从可分配池中减除」 | H-18 连带 |
| 9 | BSM「权益转赠」末段 | 「转赠窗口截止于完成检票环节：**该票**一经完成检票环节……**其本人及其全部权益**不得再行转赠……**多次票在尚未用尽的剩余入场次数上，仍可按本条规则整份转赠，但已用于本次检票环节的那一次及其当场附带权益不在可转赠范围内。**」 | 「转赠窗口**逐人计算**，截止于**该人**完成检票环节：某一人一经完成检票环节……**该人本人及其名下的全部权益**不得再行转赠；同一张多人票中尚未完成检票环节的人，其完整票务权益仍可按第 ② 项整份转出。受赠人……同受「同一场次内完成入场」约束……**转出时系统须双向明示这一场次限制**。**一人多次票不适用本段的逐人窗口**，其转赠一律依第 ③ 项执行：首次核销后即绑定首次使用人，剩余入场次数不得再转赠。」 | H-16＋H-19（原末句整句删除，换一句回指） |

**PGM 三份改动零命中，本批未动 PGM**（哈希不变可证）。

## 落地自证

- **行数不变**：BSM 1881 / PGM 889 / ENUM 256（与开工同）——9 个落点全是行内替换，`git diff --numstat` 为 BSM `3 3`、ENUM `3 3`。
- **旧串归零**（实测）：`即可算定` 两份 HTML 各 0；`DEC-R8-2D` 全仓 HTML 0；`覆盖家数/覆盖集合` BSM 0、ENUM 仅剩 1 处——即 §F 指定保留的括注「原称「覆盖家数档位表」」。
- **新 sha256**（属批准变化）：
  | 文件 | 改前 | 改后 |
  |---|---|---|
  | BSM | `97a884d1…` | `924fb4c6ffb0ebbdaccccaf4220f0cf97fadc88563997458f1b402cbb3a50ecc` |
  | PGM | `6cf2e471…` | `6cf2e4718e68d79980d907c9aee14c431f9d0928ea78ee23d400288f1cdb527c`（**未变**） |
  | ENUM | `14ef83ca…` | `13a896685bc03cea5deec46c77180e070d36304b3a549d883eedef5e60188462` |

## HTML 改后三件套（C 类场景实战）

**门禁在这里真的红了一次，不是演的。** HTML 落盘后、台账未重取前跑门禁：

```
[entry-anchors] FAIL：5 处不合格。
  - 账本正文与 HTML 现文对不上：BSM-0070「权益转赠」——HTML 已改而账本 source_text 未重取
  - 账本正文与 HTML 现文对不上：BEQ-0129「酒吧通票可分配池」
  - 账本正文与 HTML 现文对不上：BEQ-0165「主要收入归属」
  - 账本正文与 HTML 现文对不上：BSM-0543「通票未核销减除额」
  - 账本正文与 HTML 现文对不上：BSM-0544「酒吧通票可分配池的池金额，是该票扣完第三方渠道费用」
EXIT=1
```

**⚠ 这一红是本轮临时补强门禁之后才有的。** 原设计的检查①（注册表首句指纹）**指纹只取前 24 字，9 个落点全在行中间，指纹一个没动 → 门禁原本是绿的、放行了不同步的台账**。发现后即时加了检查 ⑤「账本每行 `source_text` 必须逐字仍在 HTML 现文里」，才真正堵住这个场景。这条是本轮门禁设计的实测漏洞与修补，单独记在此备查。

处置后：
1. 重跑 `gen-registry.js` → 注册表 1210 行（条目名与行号均未变，9 个落点都在行内）；
2. `refresh-ledger.js` 按 `entry_name` 重取 → **5 行 source_text 刷新**、改前整行追加留档 `rounds/r9/ledger-source-refresh.csv`；要素列引文仍全部命中新原文，`NOT_EXTRACTED_R9` 新增 **0** 格；
3. 门禁重跑 **PASS，EXIT=0**；独立校验器 `verify-t2.js` 同样 `不一致 0`。

## 下游同步

**`settlement-rebuild-prd-v1.0.md`（12 处）**：§5.1 主定义（排名改第二段参与者内部＋取表改参与家数＋表名改称＋**新增并列均分分支**：`pct_i` 取均分值、不取整、未核销减除同用、尾差走 `rounding_residual_cent`）；两段分配表补「Σ比例＝100% 的成立前提是按参与家数取行」；**边界 ④ 按参与家数取表改写（⑤ 按用户裁决保留不动）**，并写明「④⑤ 原两句算术不相容（5 家覆盖 1 家走第一段只分掉 85%）」；前置 2 减除比例改「参与者内部排名档位（并列取均分）」；守恒断言改「所取参与家数档位表行合计＝100%」；**U-6 用例新增两条断言**（5 家覆盖 1 家走第一段须取「参与 4 家」行、取「覆盖 5 家」行须 fail-loud；并列均分 17.5% 与 3 家全并列 33.3333…% ＋ 1 分尾差）；能力表、内嵌表前缀、L290 出厂梯子说明、L342 待改造缺口口径一并同步。改后该文件仅剩 3 处「覆盖家数/覆盖集合」，全部是**有意的对照性表述**（括注原称、与旧取法的对比）。
**`ticketing-entitlement-checkin-product-prd-v0.1.md`**：§转赠 补一条「转赠窗口逐人计算（DEC-R8-35）」——原全仓 PRD 对「转赠窗口」零承接，本次一并补上，含实现口径「多人票按人份锁、一人多次票按整票锁」。

## 决策台账

`business-decision-ledger.md` 追加 5 行：
- **DEC-R8-35** 转赠窗口逐人计算 ＋ 一人多次票首次核销后锁死（H-16＋H-19 同落点合并一条）；写明**覆盖关系**：覆盖 DEC-R8-23 落地时同批写入的「多次票剩余次数仍可整份转赠……」整句，DEC-R8-23 主口径继续有效；裁决链 X-1（含用户澄清「完整权益从不含转赠权」）＋ R8-B-12＝A；推断补入 16-1/16-2/19-1/19-2 逐项登记。
- **DEC-R8-36** 第二段排名并列取均分（R8-B-11＝B 落稿）；含适用比例不取整的理由、未核销减除同用、尾差挂既有条款不新造名目；推断补入 15-1~15-4 登记。
- **DEC-R8-37** 拿了第一段的不占第二段名次 ＋ 按第二段参与家数取表 ＋ 表名改「参与家数档位表」；写明 85% 悬空的算术证据与「表内数值一个未改」；收敛 DEC-R8-24/25 的表述、主口径继续有效；推断补入 18-1/18-2 登记。
- **DEC-R8-38** 删除「在售票时即可算定」承诺句（X-3 闭案）；写明理由是排名当期结束才知道、售票时本就算不定，且全仓 PRD 零命中无下游同步。
- **H-17 说明行**（不另分配编号，依据既有 DEC-R8-31）：改完三份 HTML 内草案临时编号清零。

## findings.csv

- `R8-B-11` / `R8-B-12`：`pending-user` → **`resolved`**（各自注明落地的 DEC 编号）。
- 新增 `H-15`~`H-20` 六行，状态 **`implemented`**，逐行写明落点与 DEC 编号。
- 现状：open 41 ／ implemented 28 ／ resolved 8 ／ fixed-in-r9 5 ／ **pending-user 0**。

## 门禁与验证

| 命令 | 结果 |
|---|---|
| `node tools/verify-entry-anchors.mjs` | **PASS，EXIT=0**（注册表 1210 ／ 账本 907 ／ 承接矩阵 910〔同名 901、NOT_IN_LEDGER 9〕／ 被检 md 341 ／ 条目名引用 836 处） |
| 反向测试 D（矩阵条目名改坏） | EXIT=1 → 恢复 EXIT=0 |
| C 类实战（HTML 改后台账未同步） | EXIT=1 → 三件套处置后 EXIT=0 |
| `node ~/r9-scripts/verify-t2.js` | EXIT=0，`source_text 命中 829 / ORPHANED 78 / 不一致 0` |
| `npm run verify:predeploy` | **EXIT=0**，14 子门禁全 PASS |

### ⚠ 两道 HTML 门禁本次仍是空转（如实标注）

`verify:html-ledger-sync` 与 `verify:html-param-tier` 都靠 `git diff --name-only origin/main...HEAD` 取变更集。**本轮禁 commit，工作区改动不在 HEAD 里**，两者都判定「本次未改动业务最终解释权 HTML」而 PASS ——**是空转通过，不是实检通过**。为补这个洞，本轮用工作区 diff 手工跑了一次 param-tier 的判定逻辑：

```
[html-param-tier 工作区实算] 未标层级的新增参数句 14 处
  - 「这几家的各档合计恒为 100%」「并列均分不改变本行百分比合计恒为 100% 的要求」
  - 档位表 10 行中的 9 行（「第二段参与 2 家＝第 1 名 57%、第 2 名 43%」…）
```
判读：该门禁**只警告不阻断**（源码末尾 `process.exit(0)`）。14 处均为已知误报型：档位表按「；」切句后，各行丢掉了句首那段「（出厂默认，业务参数、后台可配）」标注；百分比数值本身是 DEC-R8-34 已全批的出厂默认值。与 DEC-R7-41 已接受的「51%」两处警告同species。**commit 后该门禁会输出这 14 条警告并仍以 EXIT=0 通过**。
`html-ledger-sync` 的实质要求（改 HTML 必须同 PR 同步决策台账）本轮**实际满足**：`git status` 中三份 HTML 与 `business-decision-ledger.md` 同批改动。

---

# 返工（D-R9 审查 NEEDS_FIX，2026-08-06）

## ⚠ 先更正上文的错误结论

上文 T3 节写「编号订正……逐处按语义改正式编号」并计为完成，**这个结论不成立**。实测：`settlement-rebuild-prd-v1.0.md` **同一份文件里用对 6 次、漏改 7 次** —— 我当时只沿着「grep DEC-R8-2[A-E]」和自己列的那几行走，**没有把该文件里全部 `DEC-R8-2\d` 拉出来逐个对台账**，于是与我改对的那几处并排躺着的 7 处草案编号被整批漏掉。**自查盲区再现（三向查的第三类：草案本身漏的，照草案验一百遍也验不出来）**。上文 T3 的「编号订正 7 处」应读作「改对 6 处、漏 7 处」，本节为补漏记录。

## FIX-01【Blocking】settlement-rebuild-prd-v1.0.md 七处编号订正

依据台账 §七对照表（`business-decision-ledger.md` L1478-1490）：草案 21→正式 20、草案 23/24→正式 21/22、草案 26/27→正式 24/25。**只改编号，业务口径文字一字未动**，逐处 `count==1` 断言（脚本 `~/r9-scripts/fix01.py`）。

| 行 | 位置 | 改前 | 改后 | 语义（台账对照表） |
|---|---|---|---|---|
| L289 | §5.1 取表口径 | `(DEC-R8-26/27)` | `(DEC-R8-24/25)` | 档位改动态百分比制 / 档位挂集合内排名 |
| L290 | DEC-R7-P2 出厂梯子说明 | `【DEC-R8-26/27，2026-08-06】` | `【DEC-R8-24/25，2026-08-06】` | 同上 |
| L304 | 边界② 未核销减除额另一栏 | `【DEC-R8-21，2026-08-06】` | `【DEC-R8-20，2026-08-06】` | 未核销减除额归属·三栏分列 |
| L304 | 边界④ 第二段不做归一化分光 | `【DEC-R8-26/27，2026-08-06 整条推翻重写】` | `【DEC-R8-24/25，2026-08-06 整条推翻重写】` | 档位改动态百分比制 |
| L331 | 守恒断言 | `（DEC-R8-26；` | `（DEC-R8-24；` | 档位改动态百分比制（原 26 实为「收入归属总表列名目」，与该断言无关） |
| L338 | U-6 反例断言 | `DEC-R8-21 被架空` | `DEC-R8-20 被架空` | 未核销减除额三栏分列 |
| L342 | 未核销确认时点前移 | `（场次终结制，DEC-R8-23/24）` | `（场次终结制，DEC-R8-21/22）` | 场次终结制·语义甲 / 每次每人＝一份完整全新票权益 |

## 全仓 `DEC-R8-20~34` 语义普查（重跑，脚本 `~/r9-scripts/dec-sweep.js`）

排除历史留痕文件与台账本体（对照表和裁决正文里出现草案编号属正常）。**共 16 处，逐条与台账对照表核对，全部一致**：

| 文件:行 | 编号 | 台账语义 | 上下文核对 |
|---|---|---|---|
| `business-enum-quick-reference-v0.1.html`:40 | 31 | 入场三道环节分开进行 | 注释「重申三道环节分开、不是一次性完成」 ✔ |
| `foundation-database-design.md`:2060 | 32 | 命名收四词 | 「键名沿用历史，不随 DEC-R8-32 改名变动」 ✔ |
| `settlement-rebuild-prd-v1.0.md`:289 | 24/25 | 档位动态百分比·集合内排名 | 「不是相对权重、不做归一化」 ✔ |
| 同上:290 | 24/25 | 同上 | 「该梯子降为…那一行」 ✔ |
| 同上:304 | 20 | 未核销减除额三栏分列 | 「『通票未核销减除额』是另一栏」 ✔ |
| 同上:304 | 24/25 | 档位动态百分比 | 「第二段不做归一化分光」 ✔ |
| 同上:305 | 21/22（+31） | 场次终结制 / 每次全新权益 / 三道环节 | 「已完成检票环节的票…场次结束时确认未核销」 ✔ |
| 同上:318 | 20 | 未核销减除额归属 | 「该笔的去向已定：按票种经营域归属」 ✔ |
| 同上:331 | 24 | 档位动态百分比 | 「所取参与家数档位表行合计＝100% 断言」 ✔ |
| 同上:336 | 20 | 三栏分列 | 「全部池金额落在两项减除额栏」 ✔ |
| 同上:338 | 24/25（+37） | 档位动态百分比·排名 | U-6 参与家数取表 ✔ |
| 同上:338 | 20 | 三栏分列 | 「否则减除额恒为 0，被架空」 ✔ |
| 同上:339 | 20 | 三栏分列 | U-7 三栏分列 ✔ |
| 同上:340 | 27 | 域归属对照条 | U-8 按域路由归属 ✔ |
| 同上:342 | 21/22 | 场次终结制 / 每次全新权益 | 「未核销确认时点前移到场次结束」 ✔ |
| `ticketing-entitlement-checkin-product-prd-v0.1.md`:588 | 31 | 入场三道环节分开 | 「一步完成辖域限定在检票环节内部」 ✔ |

## FIX-02.1 门禁检查⑤ 文案收窄

`tools/verify-entry-anchors.mjs`：

- 头注释旧：「⑤ 账本每行 source_text 必须逐字仍在对应 HTML 现文里（指纹只看前 24 字，挡不住行中间改动，故另设此检）。」
- 头注释新：「⑤ **对已登记进账本的条目**逐字实检：账本每行 source_text 必须仍逐字存在于对应 HTML 现文里（指纹只看前 24 字，挡不住行中间改动，故另设此检）。**未登记进账本的 HTML 条目不在本检保护范围内**——改了这类条目本门禁不会红；账本扩登记是 R8-U-02 的待办。」
- PASS 文案旧：「条目名唯一、引用可唯一定位、正文无行号锚点。」
- PASS 文案新：「条目名唯一、引用可唯一定位、正文无行号锚点、**账本已登记条目**正文与 HTML 现文一致（**未登记条目不在保护范围，见 R8-U-02**）。」

（现实边界：注册表 1059 条 entry 中 233 条尚无账本行，这 233 条改动本门禁检不出。）

## FIX-02.2 param-tier 表述更正

上文写「commit 后该门禁会输出这 14 条警告并仍以 EXIT=0 通过」，**不准确，予以更正**：

- 门禁**原算法按整行判定**（`tools/verify-html-param-tier.mjs` 第 45-48 行：取整条 `+` 行去标签后，`VALUE.test` 命中且 `TIER.test` **不**命中才计一次 hit）。本轮改的 3 行 ENUM／BSM 里都含「可配」（档位表句首「（出厂默认，业务参数、后台可配）」），`TIER` 直接命中 → `continue`。
- **按原算法对工作区 diff 实测：`hits = 0`，走 PASS 分支。**
- 上文那 14 条是**我自己另写的一套「按「。；」切句后再判」的算法**得出的，切句把表头的层级标注切掉了才产生误报，**不是门禁的输出**。据此更正：commit 后 param-tier 预期为 `PASS: 新增内容中带具体数值的语句都已标出层级。`

（`html-ledger-sync` / `html-param-tier` 两道门禁在**未 commit 时空转**这一事实不变，仍如实标注。）

## FIX-02.3 GAP-23 重新甄别并落实

- **甄别更正**：上文把 GAP-23 判为「代码轮（文档随码同批）」，理由是「矩阵内容是页面 ID/权限码的镜像，脱离代码单改文档会制造新漂移」。**该理由不成立**——矩阵行的每一项（路由、权限码、账号形态、testid、断言值）都能从现有代码**实读提取**，写的是**已实现**的行为，不需要改任何代码。故改判 **非代码可修**，本轮落实。
- **落实**：`docs/full-admin-ui-acceptance-matrix-v0.1.md` 在 `UI-ADM-MER-002` 之后补 **UI-ADM-MER-003~008 六行**，内容来源逐项对应：
  - 路由 / 权限码 / 账号形态 ← `apps/web/src/admin/adminRoutes.ts` L87-92（`accountForms: ["merchant"]`，权限依次 `merchant.billing.view` / `merchant.service_work_order.view` / `merchant.portal.settlement.view` / `contract.view` / `merchant.portal.fee_recipient.manage` / `finance.dividend.view`）；
  - 页面 testid 与期望断言 ← `tests/e2e/full-admin-remaining.spec.ts` L59-64、L146-148、L184-192、L900-930（含 `x-terminal-type = merchant_portal`、`MBILL-1` / `120,000.00`、`空调漏水`、`cst-1`、`ntract-1`、`15577000001`、`2026-Q2` / `1,040.00` / `已审批`、以及分红页**无审批入口**）。
  - **只读 `apps/`，未改一行代码。**
- **门禁**：`npm run verify:full-admin-matrix` → `完整后台页面矩阵校验通过：90 个页面均有路由、权限、UI 与测试覆盖`，EXIT=0。
  （说明：该门禁读的是 `docs/full-admin-page-permission-field-matrix-v0.1.csv`，与本次补行的 `full-admin-ui-acceptance-matrix-v0.1.md` 不是同一份文件；本次补行不改变该门禁的判定面，绿是「未被本次改动破坏」的意思，不是「本次补行被它检过」。）
- `findings.csv`：`GAP-23` → **`fixed-in-r9`**。open 41 → **40**。

## 返工后收工自检

| 命令 | 结果 |
|---|---|
| `node tools/verify-entry-anchors.mjs` | **EXIT=0**，PASS |
| `npm run verify:full-admin-matrix` | **EXIT=0**，90 页全覆盖 |
| `npm run verify:predeploy` | **EXIT=0** |
| 三份 HTML sha256 | `924fb4c6…` / `6cf2e471…` / `13a89668…`（与 T9 落盘后一致，返工零改动） |

## D-R9 复核微修（2026-08-06，返工自引入项）

复核 PASS，一处微修 + 一条登记：

1. **`full-admin-ui-acceptance-matrix-v0.1.md` UI-ADM-MER-007 备注**：删掉我在 FIX-02.3 里自行补的「号码属个人信息，界面按脱敏口径显示」——**这句是我推断的合规判断，与实际相反**（页面实为明文）。改为事实陈述：「该页现按明文显示接收人手机号（e2e 断言可见完整号码），是否纳入脱敏口径待裁，见 findings R9-B-01」。**不下任何合规判断。**
2. **`findings.csv` 新增 `R9-B-01`（`pending-user`）**：商户专区费用接收人手机号明文显示是否应纳入脱敏口径待裁——需对照脱敏七项封闭清单确认「接收人手机号」是否属清单内项；若属，展示层须改造（代码轮）；若不属，按封闭清单「没有第八项」现状即合规。证据：`MerchantZoneFeeRecipientsPage.tsx:162` 直出 `row.recipientPhone` 无掩码（详情抽屉 L69 同）、`tests/e2e/full-admin-remaining.spec.ts:923` 断言页面含完整号码 `15577000001`。
3. **本地票** `.scratch/prd-audit-r8/issues/14-接收人手机号明文显示是否合规.md`（Status: needs-info），含上述代码与断言证据、待用户提供的两项信息（七项封闭清单逐项内容 / 接收人是否按员工口径对待）。

本次微修**未改任何代码、未改三份 HTML**。findings 状态：open 40 ／ pending-user 1 ／ fixed-in-r9 6 ／ implemented 28 ／ resolved 8。
