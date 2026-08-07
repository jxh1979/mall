# 文档修复批 worklog（引文漂移订正 + PRD 承接补写）

- 日期：2026-08-07
- 分支：`docs/quote-and-coverage-fixes`（工作区 `~/wt-docfix`），基线 `origin/main = 7da3aaa1`
- 依据：`prd-audit-r8/PRD引文漂移摸底.md`、`prd-audit-r8/代码轮欠账-非钱路现状.md`、`.claude/workflow/request.md`
- 性质：全部 A 类（照抄现行 HTML 现文 / 已批裁决），零推断扩写
- 硬边界（本批一字未碰）：三份业务 HTML、`docs/prd-audit/*.csv`（账本 / 承接矩阵 / findings / 注册表）、`docs/prd-audit/business-decision-ledger.md`、`docs/service-order-product-prd-v0.1.md`、`apps/backend`

---

## A 组 · 引文漂移订正（7 处）

| # | 文件 | 改前 | 改后 | HTML 出处 |
|---|---|---|---|---|
| A-1a | `data-archive-delete-pattern-design-v0.1.md` 表格删除行 | 是（申请→**上级审批**→执行） | 是（申请→**最高权限审批**→执行；**申请人不得审批自己的申请**） | BSM「集中数据清理工具」条 |
| A-1b | 同上 · 2026-07-07 更新块首行 | 引「原则上存档删除需要最高权限批准」（与 HTML 相反，且与本文件表格「存档→否」自相矛盾） | 首行去掉误引；新增【2026-08-07 引文订正】行，照 BSM 现文逐字重取「存档及删除都需要填写理由；存档不用审批，删除一律须由具备最高权限的人审批通过后执行，不设例外，申请人不得审批自己的申请；持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」 | BSM「集中数据清理工具」条 |
| A-1c | 同上 · ② 结论句 + 新增 ③ | ② 写「故『原则上需最高权限批准』现状已满足」 | ② 改「故『删除须由具备最高权限的人审批』现状已满足」；新增 ③ 批量存档闸提示（一次性存档超过后台配置条数、或整店整类存档须经审批），标注**本文档表格未体现 + 实现侧待核（代码轮）** | BSM「存档（先下线）」条 |
| A-2 | `platform-governance-prd-v0.1.md` §12 第 15 条 | 标「**原文**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”」 | 改「**本 PRD 早期表述**……」，并加订正括注：该句不是 HTML 原文，PGM 现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样 | PGM「未来需要集团视图和跨项目汇总视图」条 |
| A-3 | `settlement-rebuild-prd-v1.0.md` §5.1 折扣归属缺口块 | 「按各业态价格比例分摊到各域、各域主管公司各自承担或取得」 | 「凑整产生的差额按各业态价格比例，**与折扣一并**分摊到各域，由各域主管公司**按份额**各自承担或取得」 | BSM「综合／联合通票的定价构成」条（ENUM「综合通票定价」条同文） |
| A-4 | `settlement-rebuild-prd-v1.0.md` §9 高价值阈值块 | 「阈值未配置时一律按高价值处理、不得静默放行」 | 「阈值未配置时一律按高价值处理、**拦下走审批**，不得静默放行」 | BSM「高价值物品」条 |
| A-5 | `third-party-business-access-product-prd-v0.1.md` §7 | 标「原文」但把 HTML 两句合并改写为一句 | 改为「原文两句：『平台侧金额必须系统自动取数、不许手填；第三方侧接入前可人工录入但须标记并强制复核』与『渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证，复核人须比对原件与录入值后签字，无原件的录入不得进入对账结论、只能挂为待补凭证』」 | BSM「取数规则」条 |
| A-6 | `finance-settlement-product-prd-v0.1.md` §3 | 「核销/结算发生时把当时的价固化到该笔流水」（HTML 零命中，且丢后半句） | 「核销或结算时把当时价格固化到流水，**历史不得手改**」 | BSM「每个项目分别建立“内部结算价管理”页面和独立价格目录」条 |
| A-7 | `business-decision-log-v0.1.md` L909 下方 | —（**原行一字未改**，遵决策日志纯追加纪律） | 追加一行订正括注：所引「六业态收入归属」「通票收入分账全流程」「通票结算口径」三名称在三份 HTML 零命中，系账本摘要标签；真实出处 = ENUM「主要收入归属」条 + BSM「收入归属：哪类收入归哪个业态主体」章节 / ENUM「酒吧通票可分配池」条 + ENUM「域归属对照」条 | 同左 |

摸底报告 C 类（3 处历史变更日志行写 PGM 旧句）按报告结论**不动**。

## B 组 · MAIN 轨旧口径漂移修（4 处）

| # | 文件 · 位置 | 改前 | 改后 | HTML 出处 |
|---|---|---|---|---|
| B-1a | `business-html-source-of-truth-acceptance-spec-v0.1.md` L24 `HTML-CUST-01` 字段列 | 成长积分、可用积分、兑换发放状态、储值账户 | 成长值、可用积分（两套独立账）、券包状态（可用／已核销（已使用）／已过期／已失效）、储值账户 | BSM「等级与成长值」条、BSM「我的券包」条 |
| B-1b | 同上 · 边界列 | 积分两本账；**兑换先扣礼品库存**，库存不足拒绝且不扣积分；**不支持会员间转赠** | 成长值与可用积分是两套独立账、积分兑换不扣成长值；积分只兑虚拟券，**发券时扣积分并预占库存或履约能力、核销时预占才转为实际消耗（未核销不结算）**；**积分余额不可转赠，虚拟券默认不可转赠、券种可明确开放，资格类券强制不可转赠** | BSM「积分兑换」条、BSM「兑换次数与转赠」条 |
| B-1c | 同上 L60 `RULE-MEM-01` | 来源写行号锚点「业务:743-744」；规则写「成长积分只增不减用于等级……等级按**累计成长积分门槛**自动升级」 | 来源改条目名引用 `BSM「等级与成长值」条`；规则改「成长值与可用积分是两套独立账、积分兑换不扣成长值；等级按后台配置的**滚动统计周期**内的成长值计算，达到门槛立即升级，升级后享有**保护期**，保护期结束按届时滚动周期内的成长值重新定级」；验收列同步 | BSM「等级与成长值」条 |
| B-2a | `member-loyalty-and-stored-value-development-design-v0.1.md` §1 | 引「业务速查表 §二：成长积分（算等级，只增不减、仅到期清减、不受兑换影响）……」 | 改 ENUM「等级与成长值」条现文「成长值与可用积分是两套独立账，积分兑换不扣成长值」+ 滚动统计周期/保护期，并注明代码侧尚无独立成长值账、差距见该文既有待办表 | ENUM「等级与成长值」条 |
| B-2b | 同上 §3 过期清零 | 「按自然年或后台配置天数清减**成长积分**」（写成业务口径） | 标明为**代码现状**（清减 `keep_threshold_progress`），并逐字给出现行 HTML 口径：ENUM「积分明细类型」条「积分有效期为滚动有效……」；等级另按 ENUM「等级与成长值」条滚动周期 + 保护期计算 | ENUM「积分明细类型」条、ENUM「等级与成长值」条 |

### B-3 全文顺扫结果（同主题旧口径：成长积分／累计／自然年清零／默认可转赠）

- `business-html-source-of-truth-acceptance-spec-v0.1.md`：全文命中仅 L24、L60 两行，已全改。
- `member-loyalty-and-stored-value-development-design-v0.1.md`：「成长积分」共 5 处、「自然年」1 处。
  - 已改：L13（§1 引文）、L73（§3 过期清零）。
  - **有意保留**：L18 / L23 的「成长积分（保级进度）」是**代码列 `keep_threshold_progress` 的标签**；该文自己的待办表逐字写着「成长值这本账在代码里根本不存在」，把代码列改称「成长值」会与该结论直接矛盾、并把不存在的账说成存在。已在 B-2a 的订正括注里说明代码侧现状，未改标签。
  - L98 的「成长积分」不存在（该行为已废止付费会员段），无需处理。

## C 组 · PRD 承接补写（15 条 findings，13 项内容）

| findings | 内容（照抄 HTML 现文） | 落点 | HTML 出处 |
|---|---|---|---|
| REGX-01 | 单个酒吧的单店门票允许客人在自己手机上的综合体官方小程序或 App 自助购买 | `ticketing-…-prd` §2 业务边界；`customer-app-miniapp-…-prd` §3 主要功能 | BSM「门票主路径是客人通过官方 App/小程序或第三方平台自助购买」条 |
| REGX-02 | 工单由内部部门发起、任何一个员工都可以发起；发起时填写描述、证据、时间等；可指定受理部门 | `platform-governance-prd` **§15E.1（新增）** | BSM「内部工单」条 |
| REGX-03 | 受理部门必须填写处理结果并给予反馈；可将部分事项转指定其他部门协助；每张工单必须形成完整闭环，未闭环的不得关闭 | `platform-governance-prd` **§15E.2（新增）** | BSM「内部工单」条 |
| REGX-04 | 购票订单下单时间／支付时间双记，各记各的、不得互相顶替，未付款支付时间留空；用于对账、时效统计和全链追溯 | `ticketing-…-prd` §7.2 订单状态 | BSM「核销、退款与有效期规则·下单与支付时间」条 |
| REGX-05 | 退款留痕：原因、责任主体、发起人、审批人、适用规则版本、原路退回结果、对应财务/积分/成长值/权益冲正 | `ticketing-…-prd` **§8.4.2（新增字段表）** | BSM「退款授权与留痕」条 |
| REGX-07 | 酒吧点单与账单双时点（同 REGX-04 语式） | `bar-…-prd` §6 账单状态 | BSM「桌台/账单状态与经营规则·下单与支付时间」条 |
| REGX-08 | 销售提成随薪资发放（「次月」按自然月计）；薪资发放日后台可配（按项目、须同时配上下限），不得迟于次月 15 日 | `bar-…-prd` §11 已确认 **5e（新增）** | BSM「销售提成」条 |
| REGX-09 | 退款时的提成与服务费冲回三条 + 反向结算记录不覆盖原记录 | `bar-…-prd` §11 已确认 **5f（新增）** | BSM「销售提成」条 |
| REGX-10 | 内部结算价审批主体两档：餐饮零售统一城市项目公司审批；业态「其他」按商户档案指定主管公司审批；未获批前不得开通需该价格结算的权益或券 | `settlement-rebuild-prd` §2 主体协作关系与职责 | BSM「每个项目分别建立“内部结算价管理”页面和独立价格目录」条 |
| REGX-16 | 治理顺序五步：先看对象 → 再看合同 → 再看人员 → 再看设备 → 最后留痕 | `platform-governance-prd` §2.2 | PGM「治理顺序」条 |
| REGX-17 | 城市项目公司主体级排除清单（不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务或结算） | `platform-governance-prd` §5 核心业务对象·经营主体关系 | PGM「物理项目与运营公司：多对多协作，不是固定隶属层级·城市项目公司」条 |
| REGX-18 | 跨单据统一审批模型总则（指定最终审批人 + 系统按组织层级自动补全逐级审批 + 平行线另指最终审批人 + 部门预设最低起点层级 + 任一节点驳回整单退回起草人） | `platform-governance-prd` **§2A.5（新增）** | PGM「审批链不由发起人选择档位」条 |
| R10-U-01 | 开关配置每一次变更（含模块级）必须记录变更人、变更时间、变更前后取值与影响范围；留痕不属可被开关关闭的模块，被开关拒绝的访问同样留痕 | `platform-governance-prd` §6.11 第 5 条（新增） | PGM「留痕与审计记录不属于可被开关关闭的模块」条 |
| R10-U-02 | 数据的项目与商户隔离不提供关闭开关，与导出脱敏同例 | `platform-governance-prd` §7.8 | PGM「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」条 |
| R10-U-04 | 会员身份、成长值、等级和积分按已批准规则计算；后台配置只能承载规则，不能改变三份 HTML 业务口径；票务规则与商户消费业态规则不得混用 | `platform-governance-prd` **§2A.6（新增）** | BSM「会员身份、成长值、等级和积分分别按已批准规则计算」条 |

REGX-16 已按 findings 提示如实标注：与 `r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md` §0 的「开关→准入→权限」闸口链**面向不同层**（业务判断顺序 vs 接口拦截顺序），并存不冲突。

## 变更日志同步

按各文件既有惯例记录：`platform-governance-prd` §16、`ticketing-…-prd` §14、`bar-…-prd` §13、`customer-app-miniapp-…-prd` §8、`third-party-…-prd` §13、`finance-settlement-…-prd` 变更日志（该表 4 列，行按 4 列写）。
`settlement-rebuild-prd-v1.0.md`、`data-archive-delete-pattern-design-v0.1.md`、`business-html-source-of-truth-acceptance-spec-v0.1.md`、`member-loyalty-…-development-design-v0.1.md` 四份**本身没有变更日志节**，按其惯例在正文内就地标注日期与依据，未新建日志节。

---

## 待销案清单（本批不碰任何 CSV，交后续批处理）

`docs/prd-audit/rounds/r8/findings.csv` 中下列行的 `status` 仍为 `open`，承接已在本批落地，可由后续登记批销案：

| finding_id | 承接落点 |
|---|---|
| REGX-01 | `ticketing-entitlement-checkin-product-prd-v0.1.md` §2；`customer-app-miniapp-product-prd-v0.1.md` §3 |
| REGX-02 | `platform-governance-prd-v0.1.md` §15E.1 |
| REGX-03 | `platform-governance-prd-v0.1.md` §15E.2 |
| REGX-04 | `ticketing-entitlement-checkin-product-prd-v0.1.md` §7.2 |
| REGX-05 | `ticketing-entitlement-checkin-product-prd-v0.1.md` §8.4.2 |
| REGX-07 | `bar-business-operations-product-prd-v0.1.md` §6 |
| REGX-08 | `bar-business-operations-product-prd-v0.1.md` §11 已确认 5e |
| REGX-09 | `bar-business-operations-product-prd-v0.1.md` §11 已确认 5f |
| REGX-10 | `settlement-rebuild-prd-v1.0.md` §2 |
| REGX-16 | `platform-governance-prd-v0.1.md` §2.2 |
| REGX-17 | `platform-governance-prd-v0.1.md` §5 |
| REGX-18 | `platform-governance-prd-v0.1.md` §2A.5 |
| R10-U-01 | `platform-governance-prd-v0.1.md` §6.11 第 5 条 |
| R10-U-02 | `platform-governance-prd-v0.1.md` §7.8 |
| R10-U-04 | `platform-governance-prd-v0.1.md` §2A.6 |

### 建议新登记（本批只提，不写 CSV）

- **批量存档审批闸**：BSM「存档（先下线）」条的「一次性存档超过后台配置条数的，或整店、整类存档的，须经审批后执行（业务参数，按综合体可配，须同时配置上下限）」——**PRD 侧已由 `platform-governance-prd-v0.1.md` §15C.1 承接，不属零承接缺口**；缺口限于 `data-archive-delete-pattern-design-v0.1.md`（本批已在该文档 ③ 处留标注）与**实现侧是否落地（待代码轮实测）**。
- **「表演报酬」全仓 PRD 零承接**：BSM「销售提成」条的退款冲回句点名「服务费、小费与表演报酬」三类，但「表演报酬」在 `docs/**/*.md`（剔除 `_archive`、`prd-audit`）除本批新写的 `bar-…-prd` §11 5f 外**零命中**（`grep -rn 表演报酬` 实测）——其定义、归属主体、计提与发放口径全仓无承接。本批**不作任何推断**，仅登记缺口，建议后续批按 HTML 现文单独立项。

---

## 与任务书的偏差 / 有意留白

1. **REGX-10 落点**：findings 建议 `merchant-points-settlement-rules-v1.0.md` §9，任务书指定 `settlement-rebuild-prd-v1.0.md` 内部结算价节。按任务书落在 `settlement-rebuild-prd-v1.0.md` §2（该节即主体职责与内部结算价审批主体所在），`merchant-points-settlement-rules-v1.0.md` 未动。
2. **REGX-17 落点**：findings 建议 `settlement-rebuild-prd-v1.0.md` §2，任务书指定 `platform-governance-prd-v0.1.md` 主体职责节。按任务书落在 gov PRD §5 核心业务对象「经营主体关系」块。
3. **B-2 未改代码列标签**：见上文 B-3 说明，改名会与该文档自己的待办结论矛盾。
4. **`data-archive-…` 表格删除行已按软删除口径同步**（2026-08-07 返工 IMP-1）：语义格、可逆格、数据落地格三格改为与该文档 §2 及 `platform-governance-prd-v0.1.md` §15C.2 一致的软删除口径（`UPDATE delete_status='approved'` + 前台过滤 + 可恢复 + 删前校验无活引用；PIPL 法定必销与「重算替换」类仍走物理删）。原「存量超范围」分类已撤回。
5. **验收 spec 其余行号锚点**（`业务:679、712` / `治理:286` / `治理:301` / `治理:353` 等）任务书只点名 `RULE-MEM-01`，其余未一并改；建议并入后续锚点批。
6. **findings CSV 销案未做**（硬边界），见上「待销案清单」。

## 2026-08-07 返工（独立审查 NEEDS_FIX 四项）

| 项 | 位置 | 改前 | 改后 |
|---|---|---|---|
| BLK-1 | `settlement-rebuild-prd-v1.0.md` L46 | 「与**上表既有的**『独立餐饮商户由城市项目公司审批、中央餐饮供货部分仍由中央餐饮公司审批』并存」——该句 §2 表中**根本没有**（幻影交叉引用），且引文非 HTML 逐字（缺「，但」） | 改为「**BSM 同条另有**『独立餐饮商户由城市项目公司审批，**但**中央餐饮供货部分仍由中央餐饮公司审批』，两句并存不冲突（后一句已由 `merchant-points-settlement-rules-v1.0.md` §9 项目级内部结算价承接）」——出处指真实来源，引号内为 BSM 逐字 |
| IMP-1 | `data-archive-delete-pattern-design-v0.1.md` L14 | 语义「从主表物理移除该行」／可逆「❌ 不可逆」／落地「审批通过后 `DELETE`」 | 三格同步为软删除口径，与该文档 §2、`platform-governance-prd-v0.1.md` §15C.2 一致 |
| IMP-2 | `bar-business-operations-product-prd-v0.1.md` 5f | 括注「与 §10.3 的归属判定并列，不改变其口径」（把**表演报酬**也扫进 §10.3） | 限定为「其中**服务费与小费**部分与 §10.3 并列……；**表演报酬** §10.3 未涉及，本 PRD 不对其归属作任何推断」；表演报酬零承接已登记在「建议新登记」节 |
| Advisory | 本 worklog「建议新登记」节 | 批量存档闸写成「零承接」 | 补注 PRD 侧已由 gov PRD §15C.1 承接，缺口限于本设计文档与实现侧 |

### 全批文档内交叉引用自查（BLK-1 验证要求）

本批新写正文中出现的每一处交叉引用，逐条在被指向位置 grep 复核：

| 引用出处（本批新写） | 指向 | 实测 |
|---|---|---|
| `settlement-rebuild-prd` §2 新段 | `merchant-points-settlement-rules-v1.0.md` §9 项目级内部结算价 | ✅ L165 `## 9. 项目级内部结算价`，L172 含该句 |
| `ticketing-…-prd` §2 新条 | `settlement-rebuild-prd-v1.0.md` §4 | ✅ L62 `## 4. 收入类型与主体归属` |
| `ticketing-…-prd` §2 新条「下一条『工作人员平板 App 不允许代客购买门票』」 | 本文件次行 | ✅ 紧邻下一行逐字一致 |
| `ticketing-…-prd` §8.4.2 | 本文件 §8.4.1 | ✅ `#### 8.4.1 退款情形穷举与例外放行` |
| `bar-…-prd` 5e | `finance-settlement-product-prd-v0.1.md` §4B.3 | ✅ L316 `### 4B.3 付款时点的顺序校验（硬约束）`；引文已改逐字「全部付款时点……一律后台可配，本 PRD 不写死具体日期」（原写的「付款时点一律后台可配」非逐字，本次一并订正） |
| `bar-…-prd` 5f | 本文件 §10.3 | ✅ `### 10.3 服务费与小费` |
| `gov PRD` §2.2 新段 | `r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md` §0 | ✅ L10 `## 0. 统一动作前置闸口链（T2/T3 共用框架）` |
| `gov PRD` §2A.5 | 本文件 §6.5.1 / §2A.4 | ✅ L272 `#### 6.5.1 新建合同（录入 → 审批 → 生效）`；§2A.4 七类红线在 L79 |
| `data-archive-…` 表格删除行 + ③ | 本文件 §2 / `gov PRD` §15C.2 / 本文件上表 | ✅ `## 2. 为什么存档=软标记…`；gov L823 `### 15C.2 删除与删前占用检查` |
| `member-loyalty-…-dev-design` §1/§3 | 本文件待办表 | ✅ 同文件「保级窗口不是滚动十二个月」待办行 |

## 验证

- `npm run verify:predeploy` → EXIT=0（含 `verify:html-ledger-sync` PASS「本次未改动业务最终解释权 HTML」、`verify:entry-anchors` PASS、`verify:html-param-tier` PASS）
- `verify:entry-anchors` 条目名引用 **1034 处**（注册表原名 929 + 正文唯一定位 105）全部可定位；本批新增的每一处「条目名」引用均事先逐条核对 `docs/prd-audit/entry-registry.csv` 原名。
- 行尾：11 份改动文件逐一比对基线 CR 计数，`business-decision-log-v0.1.md`（CRLF 文件）新增行按 CRLF 写入，其余 10 份为纯 LF，**行尾零变化**。
- `git status --porcelain` 仅 11 份 `docs/*.md` + 本 worklog，无硬边界外文件。
- 未 commit、未 push。
