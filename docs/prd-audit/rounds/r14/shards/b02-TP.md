审计完成。以下为 shard TP 报告。

---

# R14-b02 Auditor Shard **TP** 报告

目标文件：`/home/james/complex-management/docs/third-party-business-access-product-prd-v0.1.md`（310 行 / 37084 字节 @ `ff2037a1`，实测一致，EV-TP-01）

---

## 1. 覆盖声明

| 项 | 结果 |
|---|---|
| 取证基线 | `AUDIT_BASE_SHA = ff2037a1c1e40a97e5979a5517451c7bfe409d16`；全部被审文本经 `git show "$SHA":<path>`，零工作区读 |
| CONFORMANCE | 完成。核过面：coverage 36 承接行**逐行**过（FULL 亦抽验）、BSM「第三方接入」章 22 条目全表反查、PGM「开放接口」章 8 条目全表反查、ENUM 七章 4 条目、台账 `source_text` 与 HTML 现文长度/逐字比对（BSM-0322/0325/0335/0336/0337/0401/0405、BEQ-0065~0068/0124、PGM-0122）、PRD 内 25 处 HTML 条目名引用逐个回现文解析 |
| ADVERSARIAL | 完成。核过面：§11#3 退款/no-show 全子条与 ENUM L44 逐句比对；门店映射子条与 `settlement-rebuild:899` 跨文件镜像逐字比对；退款子条与 `ticketing:424/:433` 镜像比对；§7.3 与 BSM L1705 逐句比对；§11A.1/.3/.4 与 BSM L1555/L1557 逐项比对；「后台开关」同形态表述全文检索 |
| FIRST_PRINCIPLES | 完成。核过面：§3.1 两行同向重叠、§11#7 三分规则出处回溯（三份 HTML + DEC 台账）、§4/§5/§6 建模条出处、§2 四款并列出处 |
| skill | 三关分别调 domain-modeling / grilling / ponytail-audit 的**方法**执行（未加载 skill 正文；判定不依赖 skill 输出，如实标注） |

**Counting Contract**：
- **「36 承接行」**：对象＝`html-prd-coverage.csv` 数据行（RFC4180 / `utf-8-sig`）；过滤＝`prd_path` 含 `third-party`；不去重；含多路径合并行 1 行（`ticketing;bar;third-party`）；时点 @`ff2037a1`；全表 1234 数据行。
- **「BSM 第三方接入章 22 条目」**：对象＝`entry-registry.csv` 中 `html_file=BSM` 且 `section_h2` 含「第三方接入」的 `entry_kind='entry'` 行。21 条落点为本文件，1 条（`BSM-0329`「每一段交接仍以员工扫码为主动留痕」）落点为 bar PRD §7.5——**与任务包一致，非缺口**（G-1 全表反查，EV-TP-03）。
- 两口径**不相减、不互换**。

**三关有无发现**：三关均有 finding。

---

## 2. Findings 总表

| finding_id | audit_lens | severity | disposition | 文件:条目名 | 一句话 | EV |
|---|---|---|---|---|---|---|
| R14-TP-01 | ADVERSARIAL | **P0** | A | third-party:207 §11#3「no-show」子条 | 第三方渠道退款镜像句漏承接 ENUM「售票与退款原则」条三项硬约束，字面为**已核销**渠道票开出 U 盘密钥退款通道 | EV-TP-05/06/07 |
| R14-TP-02 | CONFORMANCE | P1 | A | third-party:145-151 §7.3 修订说明 | 与 BSM「取数规则」条实质冲突却自陈「完全一致」；该条第二句三要素零承接 | EV-TP-08/09 |
| R14-TP-03 | CONFORMANCE | P1 | A | third-party §3.1 接入矩阵 | BEQ-0065 列举的「短信平台」「安检环节设备（X 光）」两类外部系统零行；BSM-0325 回流内容全仓 PRD 零承接；§11A 对 §3.1 的引用为悬空引用 | EV-TP-10/11/12 |
| R14-TP-04 | CONFORMANCE | P1 | A | `authority-rule-ledger.csv` PGM-0122 行 | 台账 `source_text` 只登第一句，缺 DEC-R8-97-06 写入 HTML 的门店映射两句——改 HTML 未同步账本 | EV-TP-13/14 |
| R14-TP-05 | CONFORMANCE | P1 | A | third-party:206 §11#3「渠道未核销票归属」 | 域归属枚举缺「餐饮域与零售域 → 城市项目公司」；未承接 ENUM「到期未核销款」条的射程限定；全子条无依据标注 | EV-TP-15 |
| R14-TP-06 | CONFORMANCE | P2 | A | third-party:39 §3.1 团购售票渠道行「平台不做」列 | 缺 BSM 第三列「完整外部店铺运营」，全仓 `docs/*.md` 零命中 | EV-TP-16 |
| R14-TP-07 | CONFORMANCE | P2 | A | third-party 4 处条目名引用 | 引用名与注册表/HTML 现文不精确一致（同源 **R14-CON-02**） | EV-TP-17 |
| R14-TP-08 | FIRST_PRINCIPLES | P2 | **B** | third-party:45/:47 §3.1 两行 | 「支付/渠道（非团购）」与「聚合支付」自陈同一入站方向却给两套回流字段；HTML 只有「聚合支付」条 | EV-TP-18 |
| R14-TP-09 | ADVERSARIAL | P2 | A | third-party §11A.4 验收 | 2026-08-19 新增 6 个订台类触发点无对应验收；「最晚到店时间前提醒」30 分钟＋上下限与 24 小时同形态却无验收条款 | EV-TP-19 |
| R14-TP-10 | CONFORMANCE | P2 | A | `html-prd-coverage.csv` BSM-0336 / BSM-0328 行 | 落点登记不全（内容在别处已承接，处置＝补登落点非补内容） | EV-TP-20/21 |
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-56，本文不再重复记载。
| R14-TP-12 | CONFORMANCE | P2 | A | third-party:206 / :55 | 「第三方渠道费」与「通路费」两层未划界；ENUM「跨域通票分配」条明文「性质不同、不可混用」 | EV-TP-23 |
| R14-TP-13 | CONFORMANCE | P2 | A | third-party:44 §3.1 视频监控行 | 术语契约不一致：写「证据备注」，HTML 与本文件 :161/:187/:221 均为「证据索引」 | EV-TP-24 |

---

## 3. A 类明细（fix-ready）

### R14-TP-01（P0）

**位置**：`third-party-business-access-product-prd-v0.1.md:207`，§11 已确认第 3 条 ·「**no-show（客人未到店）**」子条。

**现行原文逐字**：
> **no-show（客人未到店）**：未到店、忘记使用或主动放弃均不计核销，也不是退款理由；系统不得配置成已核销。合同不得改变平台"售出门票原则上不退款"的统一规则。只有票种规则明确允许、活动取消、无法履约、重复支付、法律要求这五种情形才可退款并完整冲正；「获授权特殊审批」不再作为退款理由，确需在五种情形之外退款的，只能由持授权 U 盘密钥的最高权限逐笔放行，每次只对当前这一笔生效（依据 ENUM「售票与退款原则」条；DEC-CFG-2026-08-03-12）。

**依据**：ENUM「售票与退款原则」条（L44，即 PRD 自标依据条），现文含本子条**未承接**的三句：
1. 「门票或权益一经开始核销（含部分核销）即不可退款」
2. 「通票安全例外是：客人出现重大情绪不稳定、安全问题或其他必要问题时，由夜班值班经理及以上批准免除其通票费用，此时即使已核销也予以退款，须记录原因、批准人与当时情形」
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-56，本文不再重复记载。

**风险**：本子条正是第三方渠道票退款的口径落点。第 3 句恰是 HTML 预先封堵「U 盘密钥可以退任何票」这一读法的那一句；PRD 只抄了授权部分、丢掉了限制部分，字面允许最高权限对已核销渠道票逐笔放行退款。已核销票的提供方履约份额已进分账，退款需逐段反向冲回并向渠道原路追回渠道费——不可逆账务 + 权限口径越权，判 **P0**。

**应改成什么**：在「五种情形」句之前补 ENUM L44 第 1 句核销状态闸；在 U 盘放行句之后补第 3 句限制语；并补第 2 句通票安全例外（承接形式可对齐 `ticketing:433` 现文，该处已完整承接，属正确镜像）。

**关联**：ticketing 分片同批在审，如其侧另有登记，本条为第三方渠道侧落点，不重复描述缺陷本体。

### R14-TP-02（P1）

**位置**：`:145-151`，§7.3 差异处理 · 修订说明（2026-07-28，v7 审计 P0-E 对账旁路手填封堵）。

**现行原文逐字（关键两处）**：
> - 第三方那笔**根本没有回流落库**时，不允许凭对账单手打一条差异出来——先把回流按正规通道补进系统，再对账。这条是硬拒绝，不是提示。
>
> 口径与财务域对账（决议 `R6-D07`）、业务 HTML 铁律 `RULE-FIN-SOURCE-01`（BSM「取数规则」条，原文两句：……）**完全一致**，两域不再各说各话。

**两个缺陷**：
1. **自陈「完全一致」不成立**。HTML 明文给「第三方侧接入前可人工录入」留了通道（须标记+强制复核+上传渠道对账单原件+复核人比对签字），PRD 写成硬拒绝。更严不等于一致；「完全一致」可证伪且已证伪。
2. **HTML 第二句三要素零承接**：全文无「对账单原件」「复核人比对签字」「无原件挂待补凭证、不得进入对账结论」。§11 第 4 条的 `manual_import` 兜底未要求渠道对账单原件、无签字比对、无待补凭证中间状态。

**应改成什么**：删除或改写「完全一致」断言，明确 PRD 收紧点及其授权来源；在 §7.3 或 §11 第 4 条补 BSM L1705 第二句三要素。

**关联**：与 **b01 R14-FS-05** 同源同条（BSM-0401），两侧形态不同、缺陷本体同为「未忠实承接 BSM-0401」，引用 R14-FS-05 登记本侧落点。附：「后台开关」同形态在本文件**不存在**（EV-TP-09，18 处命中逐条排除，:46/:17 各有 BSM-0324/BEQ-0066 明文授权）。

### R14-TP-03（P1）

**位置**：§3.1 入站回流方向矩阵（:37-49），及 §11A 引言 :237。

**依据与实测**：ENUM「接入的外部系统」条（L201，BEQ-0065）枚举 12 类，「安检环节设备（X 光）」「短信平台」在 §3.1 矩阵**无行**；coverage 却将 BEQ-0065 判 FULL 锚 §3.1、BSM-0325 判 FULL 锚「9. P0 验收标准」（§9 全文无短信平台字样）。G-1 全表反查：两条各仅 1 行落点均指向本文件；宽正则全仓：`安检环节设备` 分母 PRD 零命中；`验证码.*送达`/`送达结果` 分母 PRD 零命中。:237「送达状态回流按 §3.1 第三方接入范式处理」为悬空引用。

**应改成什么**：§3.1 补「短信平台」行（回流结果照 BSM-0325 现文逐字；「平台不做」列＝不自研短信通道）；补「安检环节设备（X 光）」行的存在性（依据 BEQ-0065）；**其回流范围 HTML 沉默 → INF-TP-01（B），不入 A**。

### R14-TP-04（P1）

**位置**：`authority-rule-ledger.csv` `PGM-0122` 行（line_hint=785）。

**现行 source_text 逐字**：`第三方只能访问授权范围内的数据和功能。`
**HTML 现文（PGM L785）**：上句 + 「每家门店与第三方平台门店 ID 的对应关系由招商运营在后台登记维护并留痕；回流订单按该映射认定商户，映射缺失的回流单挂差异，不得自动入账。」

**性质**：DEC-R8-97-06（2026-08-19 用户逐条批准 D6）同时改了 BSM 与 PGM 两条；BSM-0322 已同步，**PGM-0122 未同步**——正是 request §1 第 3 问的形态：改 HTML 未同步账本，门禁只检一致不检登全故未报。

**应改成什么**：PGM-0122 的 source_text 补齐 L785 现文全句（本轮只登记不落地）。连带：本文件 :205 门店映射子条依据应增引 PGM 该条（DEC-R8-97-06 明文并列两条为依据）。

### R14-TP-05（P1）

**位置**：`:206` §11#3「渠道未核销票归属」子条。

**现行原文逐字**：
> - **渠道未核销票归属**：有效期内未核销不与提供方结算；到期后按门票所属经营域处理：酒吧域归娱乐平台公司，游乐域归游乐公司，演出域归传媒公司，跨域组合通票按已确定域份额分别处理。第三方渠道费只扣一次。

**缺口**：①域归属枚举缺「餐饮与零售归城市项目公司」（ENUM L45/L46 均含），且卷首 :7「不统一归项目公司」合读会把合法归属方读成被排除项；②未承接 ENUM L45 首句射程限定（「本条只适用于从未完成检票环节的票或人份；已完成检票……按场次终结制进入分账减除，不走本条」）；③全子条无依据标注。

**应改成什么**：补「餐饮域与零售域 → 城市项目公司」；补射程限定；补标依据（ENUM「到期未核销款」条与「域归属对照」条）。

### R14-TP-06 / 07 / 09 / 10 / 12 / 13（P2，逐条 fix-ready）

| id | 位置 | 现行原文逐字 | 应改成什么 | 依据 |
|---|---|---|---|---|
| TP-06 | :39 第 5 列 | `不做第三方营销玩法` | 补「和完整外部店铺运营」 | BSM 第三列现文；`外部店铺` 分母 PRD 零命中 |
| TP-07 | :235/:219/:211/:210 | 四处条目名引用 | 分别改为注册表现名（重要事项短信通知（2026-06-29 新增）/ 所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定 / 平台使用企业微信已有会话能力，统一同步、存储、质检和审计 / 企业微信会话原文永久保存、不自动到期删除） | entry-registry 逐条解析 25 处引用 21 OK / 4 MISS；同源 R14-CON-02 |
| TP-09 | :280-284 §11A.4 | 五条验收 | 补 6 个新触发点验收；补「最晚到店时间前提醒」30 分钟+上下限+越界拒绝保存的验收 | BSM L1555 + DEC-R8-97-20；:250 已写参数无验收 |
| TP-10 | coverage BSM-0336/BSM-0328 行 | 单一落点 | BSM-0336 补登 `merchant-portal-zone:62-67`；BSM-0328 补登 `operation-traceability:104` 与 `supply-chain:225` | §6.4 纪律 2：补登落点非补内容 |
| TP-12 | :206 与 :55 | 「第三方渠道费只扣一次」 | 明确为「**销售渠道费**（第三方售票平台佣金）只扣一次」并引 ENUM「跨域通票分配」条 | ENUM L48「性质不同、不可混用」。⚠ ENUM L45 自身亦用简称，故非 PRD↔HTML 冲突，是两层混用歧义，判 P2 |
| TP-13 | :44 第 3 列 | `证据备注` | → `证据索引` | BSM L1495 现文；本文件 :161/:187/:221 均已用「证据索引」 |

---

## 4. B / C / D 明细

### R14-TP-08 —— B 类（P2，FIRST_PRINCIPLES）

§3.1 两行自陈同向、字段不同：`:45`「支付/渠道（非团购）| 支付结果、退款结果、渠道订单号、差异」与 `:47`「聚合支付 | 支付与退款结果、流水号、入账口径、对账明细（与「支付/渠道（非团购）」同一方向，按 HTML 单列一行）」。五条件自证通过（HTML 只有 BSM「聚合支付」条 L1510 一条；「支付渠道」仅见 BSM-0316 总述句，无独立条目无回流范围）。两处查证：台账宽正则零裁决落点；canonical 无同题。**候选**：(a) 合并为一行以聚合支付字段为准；(b) 保留两行写明划界（聚合支付＝支付通路；非团购售票渠道＝票务分销），回流范围分别定义；(c) 删 :45 并入 :39 团购行并改称「第三方售票渠道」对齐 HTML 条目名（可一并消解术语差）。

### R14-TP-11 —— D 类（P2，FIRST_PRINCIPLES）

`:220`「第三方片段留存周期按合规分类可配（依据合规文档 §6 C5）」。取证链已跑：三份 HTML 去标签检索零口径命中；DEC 台账 3 处命中均无关；唯一实名依据 `compliance-and-personal-information-protection-requirements-v0.1.md` §6 C5 不在 BUSINESS_TRUTH 轴也不在分母。**还差**：该合规文档的 authority provenance。**下一步**：台账按「合规/PIA/个人信息」宽正则追后继编号读到底；r7 finding third-9 原始条目；合规文档批准链。不阻塞判定（约束第三方侧行为，平台侧规则另有 BSM 明文），`D + P2` 不阻塞 PASS。

### C 类

无。§4.2 两种情形在本文件均未命中。

---

## 5. 推断补写清单

### INF-TP-01（P2，属 B，不入 A）

§3.1 补「安检环节设备（X 光）」行所需的**回流结果范围**与「平台不做」边界。HTML 明确到：BEQ-0065 列其为接入外部系统之一；入场三道环节第一道是安检。未明确：回传什么、平台不做什么、优先级、是否属合规必备 P0；BSM 接入表（L1470-1525 十一行）无 X 光行。候选影响：(a) 比照年龄核验设备行给最小回流→触发 PIPL 面评估；(b) 只登接入关系不承接回流→矩阵行内容为空；(c) 判 BEQ-0065 该项属设备清单而非回流对象→需 C 类改 ENUM 措辞。

其余：无第二条。§11#3 团购券 fail-closed 闸经逐句核验可由 BSM「未完成规则映射的券不得现场核销」直接蕴含，属忠实展开非推断补写，不列入。

---

## 6. R13 追溯表

`git log --oneline 2bc7cb0f..ff2037a1 -- <本文件>` 实测 **2 个 commit**（任务包候选中 `32b63708`/`1c834772` 实测未触碰本文件；以实测为准）。

| commit | hunk | 改动 | 追溯 | 结论 |
|---|---|---|---|---|
| `f843eb70`（#380） | 版本号 V0.1→V0.02 | 文档管理动作 | 无需业务依据 | 通过 |
| `f843eb70` | §9 P1 验收**删除类**：删「…或忽略。」→ 展开为主管更正/作废冲正二选一+填原因留痕+未处置不得进对账结论与锁定日结 | ✓ ENUM「回流结果状态」条 L204「不设「忽略」类终态」+「异常状态的统一出口」条 L213。**复原被删内容＝「或忽略」出口，删对了**（正是 HTML 明文禁止的终态） | 通过 |
| `f843eb70` | §13 变更日志 | 自洽 | 通过 |
| `3f3e30cd`（无 PR 直推） | V0.02→V0.03 | 同上 | 通过 |
| `3f3e30cd` | §11#3 新增两子条（团购券五判据映射+fail-closed 闸；门店映射与回流商户认定） | ✓ BSM「第三方售票渠道」条逐字（EV-TP-04）+ DEC-R8-97-06 | 通过 |
| `3f3e30cd` | §11A.1 新增 6 行订台类触发点 | ✓ BSM 短信提醒条现文 + DEC-R8-97-20 | 通过 |
| `3f3e30cd` | §11A.3 补「定金没收、定金原路退回」 | ✓ 同上 | 通过 |
| `3f3e30cd` | §13 变更日志 | 自洽 | 通过 |

**TP-S1 专项 —— BSM「第三方售票渠道」条逐句承接表**（7 分句）：分句 1~5 全部承接（2/3/4/5 逐字）；分句 0 术语不一致见 TP-08；分句 6 缺「完整外部店铺运营」→ **TP-06**。与 `settlement-rebuild:899` 镜像比对：两侧不冲突（settlement 等义改写+「唯一来源」限定；本文件逐字引用），本文件无残留推断分支（`反查|推断|回传门店号` 零命中）。分句 4/5 第二依据 PGM 条见 TP-04。

---

## 7. CODE-FOLLOWUP 候选

无。**本 shard 未执行实现侧取证**（范围声明，不是「已确认无缺口」）；73 条基线未在本 shard 内比对。

---

## 8. Evidence 附录

全部命令在 `/home/james/complex-management` 执行，基线 `@ff2037a1`（＝`ff2037a1c1e40a97e5979a5517451c7bfe409d16`）。**无 artifact 落盘能力（纯只读，无写工具），「完整输出」列一律给可复现命令**，如实标注。

| EV | 类型 | audit_lens | 命令 / file:line | exit @SHA | 关键节选（≤10 行） |
|---|---|---|---|---|---|
| EV-TP-01 | `BASELINE_STATE` | — | `git show ff2037a1:docs/third-party-business-access-product-prd-v0.1.md \| wc -lc` | 0 @ff2037a1 | `310  37084` |
| EV-TP-02 | `BASELINE_STATE` | CONFORMANCE | `python3` RFC4180 解析 `git show ff2037a1:docs/prd-audit/html-prd-coverage.csv`，过滤 `prd_path` 含 `third-party` | 0 | 全表 1234 数据行；命中 36 行；`prd_path` 分布：单文件 35 + 三文件合并 1 |
| EV-TP-03 | `NEGATIVE_SEARCH` | CONFORMANCE | 同上脚本，逐 `rule_id` 全表反查落点。检索面＝coverage 全表 1234 行；检索式＝`d['rule_id']==rid` 精确匹配 | 0 | `BSM-0329 → docs/bar-business-operations-product-prd-v0.1.md §7.5 FULL`（1 行，非本文件）；`BEQ-0065 / BSM-0325 / BSM-0336 / BSM-0322` 各 1 行且均指向本文件 |
| EV-TP-04 | `BUSINESS_AUTHORITY` | CONFORMANCE | `git show ff2037a1:docs/business-system-map-for-operations-v0.1.html \| sed -n '1465,1560p' \| sed 's/<[^>]*>//g'` 与台账 `BSM-0322.source_text` 比对 | 0 | HTML 与 `source_text` **逐字一致**：「……未完成规则映射的券不得现场核销，只能登记差异，按三方对账流程处理。每家门店与第三方平台门店 ID 的对应关系由招商运营在后台登记维护并留痕；回流订单按该映射认定商户，映射缺失的回流单挂差异，不得自动入账。 先不做第三方营销玩法和完整外部店铺运营。」 |
| EV-TP-05 | `BUSINESS_AUTHORITY` | ADVERSARIAL | `git show ff2037a1:docs/business-enum-quick-reference-v0.1.html \| sed -n '44p' \| sed 's/<[^>]*>//g'` | 0 | 「门票或权益一经开始核销（含部分核销）即不可退款；通票安全例外是：……由夜班值班经理及以上批准免除其通票费用，此时即使已核销也予以退款……最高权限逐笔放行不解除已开始核销后的退款禁止，因此不存在同一张订单既"部分核销"又"部分退款"的情形。」 |
| EV-TP-06 | `NEGATIVE_SEARCH` | ADVERSARIAL | 检索面＝`ff2037a1` 的 `docs/*.md`（排除 `prd-audit/`）；检索式＝宽正则 `git grep -nE "安全例外|情绪不稳定|夜班值班经理|一经开始核销"` | 0 | 本文件命中数 **0**；`ticketing-entitlement-checkin-product-prd-v0.1.md:424/:433` 完整承接三句 |
| EV-TP-07 | `BUSINESS_AUTHORITY` | ADVERSARIAL | `git show ff2037a1:docs/ticketing-entitlement-checkin-product-prd-v0.1.md \| sed -n '424p;433p'` | 0 | :424「只要开始核销（含部分核销）一律拒绝退款，唯一例外是 §8.4.1 的免单退款……因此不存在同一张订单既「部分核销」又「部分退款」」 |
| EV-TP-08 | `BUSINESS_AUTHORITY` | CONFORMANCE | `git show ff2037a1:docs/business-system-map-for-operations-v0.1.html \| sed -n '1705p' \| sed 's/<[^>]*>//g'` | 0 | 「取数规则平台侧金额必须系统自动取数、不许手填；第三方侧接入前可人工录入但须标记并强制复核；……渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证，复核人须比对原件与录入值后签字，无原件的录入不得进入对账结论、只能挂为待补凭证。」 |
| EV-TP-09 | `NEGATIVE_SEARCH` | CONFORMANCE | 检索面＝本文件全文 @ff2037a1；检索式＝`grep -nE "开关|可配|默认"` | 0 | 18 处命中逐条查看；无「人工录入/手填由后台开关控制」形态（R14-FS-05 同形态不存在） |
| EV-TP-10 | `BASELINE_STATE` | CONFORMANCE | 同 EV-TP-03 脚本，`BEQ-0065` / `BSM-0325` 全表落点 | 0 | 各 1 行；`BSM-0325 → third-party ... | 9. P0 验收标准 | FULL` |
| EV-TP-11 | `NEGATIVE_SEARCH` | CONFORMANCE | 检索面＝`ff2037a1` 的 `docs/*.md`（排除 `prd-audit/`、`*change-log*`）；检索式＝`git grep -lE "安检环节设备"`、`git grep -nE "短信平台"`、`git grep -nE "送达(结果|状态)"` | 0 | `安检环节设备` 分母 PRD **零命中**；`短信平台` 仅本文件 :237/:249/:294 与非分母的 `third-party-sms-notification-...`；`送达结果|送达状态` 分母 PRD 仅本文件 :237 |
| EV-TP-12 | `BUSINESS_AUTHORITY` | CONFORMANCE | 台账 `BEQ-0065` / `BSM-0325` `source_text` | 0 | BEQ-0065 枚举 12 类含「安检环节设备（X 光）」「短信平台」；BSM-0325「验证码下发与送达结果、失败原因（用于新人判定、注册登录和关键操作验证）……不自研短信通道。」 |
| EV-TP-13 | `EVIDENCE_CONFLICT` | CONFORMANCE | 台账 `PGM-0122.source_text` vs `git show ff2037a1:docs/platform-governance-map-for-operations-v0.1.html \| sed -n '785p' \| sed 's/<[^>]*>//g'` | 0 | 台账＝`第三方只能访问授权范围内的数据和功能。`；HTML 现文另含「每家门店与第三方平台门店 ID……不得自动入账。」两句 |
| EV-TP-14 | `BUSINESS_AUTHORITY` | CONFORMANCE | `git show ff2037a1:docs/prd-audit/business-decision-ledger.md \| grep -nE "R8-97-06"` | 0 | `:2109 DEC-R8-97-06 … 依据栏：BSM「第三方售票渠道」；PGM「第三方只能访问授权范围内的数据和功能」 | 用户 2026-08-19 逐条批准（D6）` |
| EV-TP-15 | `BUSINESS_AUTHORITY` | CONFORMANCE | `git show ff2037a1:docs/business-enum-quick-reference-v0.1.html \| sed -n '45p;46p' \| sed 's/<[^>]*>//g'` | 0 | L45「本条只适用于从未完成检票环节的票或人份；已完成检票环节……不走本条。……余额按票种经营域归属……**餐饮与零售归城市项目公司**」；L46「……**餐饮域与零售域 → 城市项目公司**……」 |
| EV-TP-16 | `NEGATIVE_SEARCH` | CONFORMANCE | 检索面＝`ff2037a1` 的 `docs/*.md`；检索式＝`git grep -nE "外部店铺"` | 0 | 唯一命中 `docs/prd-audit/rounds/r7/html-map-bsm-2.md:1964`（历史映射文件）；27 份分母 PRD 零命中 |
| EV-TP-17 | `BASELINE_STATE` | CONFORMANCE | `python3` 解析 `entry-registry.csv`，对本文件 25 处 `(BSM|PGM|ENUM)「…」` 引用逐个精确匹配 | 0 | 21 OK / 4 MISS：`BSM 重要事项短信通知`（现名带「（2026-06-29 新增）」）、`PGM 所有导出的文件一律带密码`（现名更长）、`PGM 平台使用企业微信已有会话能力`（现名更长）、`BSM 已拍板：企业微信会话原文永久保存`（注册表名去「已拍板：」前缀，HTML L1975 原文含该前缀） |
| EV-TP-18 | `BASELINE_STATE` | FIRST_PRINCIPLES | 台账全表检索 `entry_name=='聚合支付'`；HTML BSM L1470-1525 十一行接入表 | 0 | 仅 `BSM-0323` 一条；接入表无「支付/渠道（非团购）」行；「支付渠道」仅见 `BSM-0316` 总述句 |
| EV-TP-19 | `BASELINE_STATE` | ADVERSARIAL | 本文件 :247/:250 vs :280-284 | 0 | :250 定义 30 分钟＋上下限；§11A.4 五条验收无对应条款 |
| EV-TP-20 | `BASELINE_STATE` | CONFORMANCE | `git grep -nE "异议期" ff2037a1 -- 'docs/*.md'` | 0 | `merchant-portal-zone-prd-v0.1.md:62-67` 六项机制逐条承接 BSM-0336（适用范围/起算/长度默认 10 工作日/提醒不影响起算/全天不可用不计入 DEC-R8-88/不顺延与未读提醒） |
| EV-TP-21 | `BASELINE_STATE` | CONFORMANCE | `git grep -nE "被动留痕" ff2037a1 -- 'docs/*.md'` | 0 | `operation-traceability-and-terminal-switch-prd-v0.1.md:104`、`supply-chain-inventory-product-prd-v0.1.md:225` 承接 BSM-0328 |
| EV-TP-22 | `NEGATIVE_SEARCH` | FIRST_PRINCIPLES | 检索面＝三份 HTML 去标签全文 + `business-decision-ledger.md` 全文；检索式＝`grep -nE "留存周期|合规分类|片段"` / `grep -nE "留存周期|视频.*留存|片段"` | 0 | 三份 HTML 无「留存周期/合规分类」口径（BSM 3 处「监控片段」均为证据索引语境）；DEC 台账 3 处命中均与本规则无关 |
| EV-TP-23 | `BUSINESS_AUTHORITY` | CONFORMANCE | `git show ff2037a1:docs/business-enum-quick-reference-v0.1.html \| sed -n '48p' \| sed 's/<[^>]*>//g'` | 0 | 「渠道费用分两种，性质不同、不可混用：一是通路费……二是销售渠道费——第三方售票平台收取的佣金……在通路费之上再扣一层。」 |
| EV-TP-24 | `BUSINESS_AUTHORITY` | CONFORMANCE | 台账 `BSM-0320.source_text`；`git grep -nE "证据备注" ff2037a1 -- 'docs/*.md'` | 0 | HTML「摄像头点位、事件时间段、**证据索引**、调阅记录。」；`证据备注` 分母 PRD 仅本文件 :44 |
| EV-TP-25 | `BASELINE_STATE` | — | `git log --oneline 2bc7cb0f..ff2037a1 -- docs/third-party-business-access-product-prd-v0.1.md` | 0 | `3f3e30cd docs(prd-sync): DEC-R8-98 下游 PRD 同步 16 份文档` / `f843eb70 docs(prd): close final R13 P0 findings (#380)` —— 共 2 个，任务包候选中 `32b63708`/`1c834772` 未触碰本文件 |
| EV-TP-26 | `BASELINE_STATE` | CONFORMANCE | `git show ff2037a1:docs/business-system-map-for-operations-v0.1.html \| sed -n '1709p'` vs 本文件 :49 | 0 | BSM-0405 六态 + 「开票金额≤已收−已退−已开，退款后先红冲再重开」+「不自建开票引擎，只做状态对接与留痕」—— 本文件 :49 逐项承接，**无缺口** |

---

## 附：本 shard 未采信 / 未使用的来源

- `acceptance-spec` 多次命中，按红牌未作 BUSINESS_TRUTH 引用，仅作命中噪音排除。
- `business-decision-log-v0.1.md` 未作判准。
- 未产生任何 `RUNTIME_OBSERVED` / `MIGRATION_DECLARED` / `WIRING_ACTIVE` 证据，无 H3 态判定。
- 零写入（无写工具，plan 模式），全部产出在本报告正文内。

---

*编排者落盘备注：TP 子代理返回报告的原样落盘（§8 EV 表以要点忠实转录 + 可复现命令为源，其余逐字；HTML 实体已还原）。落盘时间 2026-08-20。*
