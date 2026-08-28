# 备份 vs main 全量逐句差异清单

- 备份目录：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs`
- main：`/home/james/complex-management` @ `18092eaa`
- **积分/会员相关文件：整文件跳过（以 main 新文档为准）**
- 标记：`-` = 仅备份有（main 已删/改走）；`+` = 仅 main 有（main 新增/改后）
- HTML 已去标签后按句子/短句对齐；Markdown/CSV 按非空行对齐
- 这是完整差异清单，便于你人工裁定；**默认权威仍是 main + 三份现行 HTML**

## 0. 总览

| 项 | 数量 |
|---|---:|
| 备份清单文件 | 38 |
| 跳过（积分/会员） | 2 |
| 字节完全一致 | 20 |
| **有差异（下文逐项）** | **16** |
| 路径异常 | 0 |

### 跳过文件

- `member-loyalty-and-stored-value-product-prd-v0.1.md`
- `stored-value-membership-checkin-birthday-prd-v0.1.md`

### 完全一致（无逐句差异）

- `admin-page-traceability-clickthrough-audit-v0.1.md`
- `building-operations-prd-v1.0.md`
- `business-dashboard-metrics-product-prd-v0.1.md`
- `business-system-map-for-operations-v0.1.html`
- `customer-app-miniapp-product-prd-v0.1.md`
- `evidence-risk-control-product-prd-v0.1.md`
- `external-evidence-requirement-traceability-v0.1.md`
- `full-admin-remaining-requirements-code-matrix-v0.1.csv`
- `merchant-portal-zone-prd-v0.1.md`
- `p0-action-event-evidence-matrix-v0.1.md`
- `p0-openapi-operation-acceptance-matrix-v0.1.md`
- `p0-operation-permission-matrix-v0.1.md`
- `platform-governance-map-for-operations-v0.1.html`
- `r1-requirement-api-page-table-test-mapping-matrix-20260618.md`
- `real-estate-full-chain-domain-prd-v0.1.md`
- `requirements-code-implementation-matrix-v0.1.csv`
- `settlement-rebuild-prd-v1.0.md`
- `supply-chain-inventory-product-prd-v0.1.md`
- `third-party-business-access-product-prd-v0.1.md`
- `web-mobile-app-terminal-extension-product-prd-v0.1.md`

---

# 一、HTML 文件（完整逐句）

## HTML｜`business-enum-quick-reference-v0.1.html`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/business-enum-quick-reference-v0.1.html`
- main：`docs/business-enum-quick-reference-v0.1.html`
- backup SHA-256：`d5f6ba58d31141d7c0bc389b254bf751f46776a56381f3d400b5791c9aa355cf`
- main SHA-256：`3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648`
- 大小：24968 → 26038 bytes

- 对齐单元数：备份 205 / main 210

- 差异块数：4
- 相似度（序列比）：0.9687

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 通票分账全流程 通票收入 → 先扣第三方渠道抽成 → 再扣赠酒成本 → 进可分配池 → 项目公司留存（默认 20%、可配） → 各酒吧总池 → 各酒吧按“排名档位 / 每客单价 / 固定额”三选一分配；

**main 现行：**

  + 通票分账全流程 通票收入 → 先扣第三方渠道抽成 → 再扣门票综合服务费（归项目公司） → 进可分配池 → 项目公司留存（默认 20%、可配） → 各酒吧总池 → 各酒吧按“排名档位 / 每客单价 / 固定额”三选一分配（各酒吧从总池的分成按整月排名 / 客数在月度结算结出）；

### 差异项 2｜双方改写（replace）｜备份 2 条 → main 6 条

**备份原文：**

  - 通票赠酒两段结算 第一段：每核销一杯先留 3 元酒成本补偿酒吧（没核销不留）；
  - 第二段：剩余按各酒吧协议（每客单价 / 排名档位）分给酒吧

**main 现行：**

  + 通票赠酒结算 赠酒核销后平台按内部结算价（成本价）与出品的这家酒吧结算——核销一杯结一杯、没核销不结算；
  + 赠酒成本平台承担，不进可分配池、不计销售提成
  + 核销顺序 同一酒吧商户，客人须先核销入场权益、再核销这家酒吧的酒水（赠饮）权益，不能跳过入场直接核酒水；
  + 未先核入场即核酒水一律拒绝
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-13，本文不再重复记载。
  + 当前游乐设施全由项目公司自营，现阶段游乐核销收入全部归项目公司、不分给商户，将来引入独立游乐经营方再按项目切分

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 押金退还须运营在确认入口手动确认后才落账，同一合同结算只算一次、不重复出账

**main 现行：**

  + 退租 / 提前终止 / 合同到期三种终止统一走多级审批（财务、运营等多部门），审批全部通过后系统自动落账退还押金，同一合同结算只算一次、不重复出账

### 差异项 4｜仅 main 有（main 新增）｜共 1 条

  + 2026-07-22 口径更新：赠酒改按内部结算价直付出品酒吧（D5、删两段式）、门票综合服务费按票种归项目公司分账前先扣、退租 / 提前终止 / 合同到期三种终止统一多级审批自动落账退押金、核销顺序入场先于酒水、游乐现阶段全归项目公司。

**本文件小计：** replace 块 3；仅备份条数 0；仅 main 条数 1；差异块 4

---

# 二、其他有差异文件（完整逐行/逐句）

## 文件｜`amusement-business-operations-product-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/amusement-business-operations-product-prd-v0.1.md`
- main：`docs/amusement-business-operations-product-prd-v0.1.md`
- backup SHA-256：`5502f380128d156951e5505ee4e93f7c5d165d3ef1a86389b5710edd62eb8a0d`
- main SHA-256：`3bafaa58aa307a38b3de2bcd6ae616c9c73ecf069da14165cd1a792c254215cc`
- 大小：18569 → 20870 bytes

- 对齐单元数：备份 185 / main 212

- 差异块数：3
- 相似度（序列比）：0.9320

### 差异项 1｜仅 main 有（main 新增）｜共 24 条

  + ### 7.2A 多限制冲突与监护（DEC-ADM-PRIORITY-001，2026-07-28）
  + > 闭合审计 ADV-ED-003。用户选择 **方案 2：固定优先级链，只报第一条失败**（ADM-PRIORITY-CHAIN）。
  + > 身高/年龄/是否需监护等阈值一律后台可配（DEC-CFG-ALL-NUMERIC-001），禁止写死。
  + #### 校验链（默认顺序，顺序本身可配）
  + 按下列顺序**逐项**校验，**命中第一条失败即停止并拒绝**，响应只返回该条失败码/原因（可附规则版本号便于排障）；不一次返回全部失败项。
  + | 序号 | 检查项 | 失败时 |
  + |---:|---|---|
  + | 1 | 项目/场次/点位可开放且未因安全关闭、暂停、故障阻断 | 拒绝核销/入项 |
  + | 2 | 安全告知/风险知悉已按项目要求完成确认 | 拒绝 |
  + | 3 | 身高限制（可配区间/下限） | 拒绝 |
  + | 4 | 年龄限制（可配区间/下限） | 拒绝 |
  + | 5 | 健康声明/禁忌项（项目配置的必答或必声明） | 拒绝 |
  + | 6 | **监护要求**（见下） | 拒绝 |
  + #### 监护要求
  + - 当项目规则判定「需要监护人」时（通常按年龄阈值可配，也可按项目强制）：
  + - 必须存在**合格监护关系/到场确认**（绑定方式与凭证类型可配）；
  + - 缺失、过期、与未成年人不匹配 → 在本链第 6 步失败并拒绝。
  + - 监护人不替代身高/年龄等客观限制：链上更前项已失败时，不进入监护判断。
  + #### 明确不做（本裁决下）
  + - 不做「多失败原因一次性全部返回」（那是 ADM-DENY-ANY 展示策略）。
  + - 不做默认「主管一键豁免越过限制」；若未来要豁免，须单独裁决（双人、留痕、范围），不得默认打开。
  + #### 终端与证据
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-13，本文不再重复记载。
  + - 失败与成功均留痕：项目、场次、点位、游客/权益、命中规则项、操作人、时间。

### 差异项 2｜仅 main 有（main 新增）｜共 2 条

  + ### 8.1A 经营数据展示口径（指针）
  + 游乐经营看板/管理层指标的数据可用性与近似展示，遵循 `data-analysis-layer-spec-v0.2.md` §F（**DEC-DATA-FRESH-PRAGMATIC-001**：有数优先，异常可用近似，轻量提示即可，不强制逐卡精密新鲜度）。

### 差异项 3｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §8.1A | 数据近似展示指针 ADV-ED-008 |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 27；差异块 3

---

## 文件｜`bar-business-operations-product-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/bar-business-operations-product-prd-v0.1.md`
- main：`docs/bar-business-operations-product-prd-v0.1.md`
- backup SHA-256：`cef43fa015e255977a1b243015177256d3782a7d9c7952044f8429ccb097439f`
- main SHA-256：`1dfb577eca9801850f9472fbccd59aa2f123f0dbaf688e2ed9e63e66d333d76d`
- 大小：45916 → 53053 bytes

- 对齐单元数：备份 350 / main 403

- 差异块数：7
- 相似度（序列比）：0.9216

### 差异项 1｜仅 main 有（main 新增）｜共 30 条

  + ### 7.3A 卡座转台与并台（DEC-SEAT-TRANSFER-001 / DEC-SEAT-MERGE-001，2026-07-28）
  + > 闭合审计 ADV-TX-007。用户确认：**有转台、有并台；无「续卡/续时」产品规则**（不做该能力，文档与实现均不得偷偷做续时重置/续费会话）。
  + > 数值类（低消额、名额、提成率等）仍遵循可配原则（DEC-CFG-ALL-NUMERIC-001），本节定结构不写死金额。
  + #### 7.3A.1 转台（原卡 → 新卡）
  + 适用：同一酒吧经营会话中，已开台卡座/桌台客人改坐另一卡座/桌台（转台）。
  + 1. **低消进度结转**：原台已累计计入低消的消费进度**直接转到新台**，不因转台清零。
  + 2. **按新台规则匹配**：
  + - 以**新台当前适用的最低消费规则**为目标；
  + - 用结转后的低消进度与新台低消要求比较：
  + - **进度 ≥ 新台低消**：视为已达标，**多出部分不退**（不因低消「超交」退差）；
  + - **进度 < 新台低消**：**不足部分须补**，补足前按新台规则限制提交/关台（与既有低消未达标处置一致，见平板低消与 §10.1 类路径）。
  + 3. **入场名额**：转台后以**新台卡座预约入场名额规则**为准重新校验在场免通票名额；原台名额不叠加保留。
  + 4. **账单与销售归属**：
  + - 转台前已产生的消费明细**保留原销售归属**；
  + - **相关销售提成不变**（不因转台重算、不改归属人）。
  + 5. **服务人员提成/服务责任**：
  + - 服务责任链随客人转到新台；
  + - **服务员提成记到新台会话**（服务侧跟台走）；销售侧提成按上款不变。
  + 6. **权限与留痕**：转台须授权岗位操作；记录原台、新台、操作人、时间、结转低消进度、新台低消目标、是否需补差。
  + #### 7.3A.2 并台（多台 → 一台）
  + 适用：多个已开台卡座/桌台合并为同一主台会话。
  + 1. **低消加总**：合并后低消要求 = **各原台当时适用低消要求之和**（加总）；已累计低消进度亦**加总**到主台后，再与加总低消比较多不退、少要补（同转台比较逻辑）。
  + 2. **销售归属不变**：历史及并台后新产生明细仍按开单时的销售归属记录；**不因并台改写销售归属**。
  + 3. **服务员提成各记**：各服务员既有服务/提成记录**分别保留**；并台不把他人提成合并到一人，也不重算历史提成。
  + 4. **名额**：主台名额规则与加总策略若冲突，以主台配置为展示主规则，并台操作时系统提示名额校验结果；具体名额是否允许加总为可配项，默认**不自动加总免通票名额**（安全默认），除非项目开启「并台名额加总」。
  + 5. **权限与留痕**：并台须授权；记录来源台列表、主台、低消加总过程、操作人。
  + #### 7.3A.3 明确不做
  + - **无续卡 / 续时规则**：不提供「同一会话续费延时并自动延续/重置低消」的独立产品能力。
  + - 需要继续消费：在**当前已开台会话**内正常加单；或**关台后重新开台**（新开台按新规则，不继承已关台进度，除非走本节目的转台/并台）。
  + - 禁止用「续时」名义绕过关台清台与低消结清。

### 差异项 2｜仅 main 有（main 新增）｜共 3 条

  + - 转台：原台低消进度结转新台，按新台规则多不退少要补；销售提成不变，服务员提成跟新台。
  + - 并台：低消要求与进度加总；销售归属不变，服务员提成各记。
  + - 不提供续卡/续时产品规则。

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 8 条

**备份原文：**

  - - **权限**：撤单为高危动作，需具备退菜退酒权限（`bar.order_item.void`）；高价值退酒须主管审批。

**main 现行：**

  + - **权限**：撤单为高危动作，需具备退菜退酒权限（`bar.order_item.void`）；已出品且达高价值阈值的退酒须主管审批（`bar.order_item.void_decide`，申请人不得自批）。
  + - **审批时机与阈值（业务确认 2026-07-27，方案 A1；编码未改前为现行口径目标）**
  + - **只审已出品**：仅 `void_type=return`（已出品 served/delivered）才进入“是否先审后行”判定；**出品前 cancel 不因本条要求先审后行**。
  + - **高价值定义**：单笔退品金额（明细 `amount_cent`）**超过**后台可配阈值时视为高价值。默认阈值 **5000 元**（= `500000` 分）；配置键建议 `bar.void.high_value_threshold_cent`，可按项目/门店覆盖，只允许按运营授权调整，不得在代码写死。
  + - **A1 低价值（已出品且金额 ≤ 阈值）**：允许**即时生效**（库存/账单副作用可立即施加），须原因码 + 操作权限 + 留痕；`approval_required` 可 false 或仅作抽检配置，不阻塞生效。
  + - **高价值（已出品且金额 > 阈值）**：**先审后行**——审批通过前不得把明细标为已撤单、不得回补/扣减库存、不得写 waiver/refund 等资金减项；仅落 void 申请（pending）与必要申请留痕。审批通过后同一业务闭环内再生效；驳回则申请关闭且无资金/库存冲销（因尚未生效）。
  + - **双证据（高价值已出品）**：生效前须满足双证据（至少：结构化原因码 + 附件证据；附件不可空）。低价值不强制双证据。
  + - **与实现漂移的关系**：若运行实现仍为“先生效后审批/驳回再冲销”，视为**实现未对齐本条**的残余风险（参见 `docs/dev-plan-v7-audit-20260722.md` P0-C / FLOW-07），不得反向把本条改回先生效。

### 差异项 4｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - - 撤单默认 `approval_required=true`（默认需审批留痕）；低值且出品前撤单可按后台配置降为免审批。

**main 现行：**

  + - 审批默认：已出品且 > 高价值阈值 → 强制先审后行；已出品低价值 → A1 可即时生效；出品前 cancel → 不按高价值先审规则强制（仍须权限+原因+留痕；是否另配抽检审批由后台可选，默认不阻断）。

### 差异项 5｜仅 main 有（main 新增）｜共 2 条

  + - 已出品且金额 > 阈值：申请后账单/库存未变，审批通过后再生效；无双证据不得生效；申请人不能自批；
  + - 已出品且金额 ≤ 阈值：可即时生效并留痕；

### 差异项 6｜双方改写（replace）｜备份 1 条 → main 10 条

**备份原文：**

  - #### 10.2.2 反结已结账单升 P0 `R6-D14`

**main 现行：**

  + #
  + ### 10.2.1A 出品-库存-赠酒-退单守恒（文档口径，2026-07-28）
  + > 与 §10.2.1（BAR-VOID-A1）配套。本小节只定义**业务守恒式与对账抽检**；编码实现另开，不在本审计线程授权。
  + **守恒式（单店/单班次可抽检）：**
  + 1. `期初可售库存 + 入库/调入 - 出品消耗 - 已出品且不可回库损耗 - 盘亏 + 盘盈 + 回库 = 期末可售库存`
  + 2. `已支付酒水明细金额（含套餐拆分后） = 客人实付分摊 + 赠酒/免单/waiver 金额 + 退品冲减（已生效） ± 授权调账`
  + 3. `赠酒/免单` 必须有原因码、权限与留痕；不得无单出库。
  + 4. `退品`：未出品 cancel 不计入出品消耗；已出品 return 按是否回库分别进入「回库」或「损耗」；**高价值已出品退酒**遵循 BAR-VOID-A1 **先审后行**（默认阈值 >5000 元可配，仅已出品）。
  + **对账抽检最小集：** 班次结束抽检未关闭 void 申请、负库存、无单出库、已生效退品与库存流水不一致。
  + ### 10.2.2 反结已结账单升 P0 `R6-D14`

### 差异项 7｜仅 main 有（main 新增）｜共 2 条

  + | 2026-07-27 | 改 | §10.2.1 退菜退酒审批时机 | 业务确认方案 A1：仅已出品进入高价值判定；默认阈值超过 5000 元（可配置，单位分）；高价值先审后行+双证据；低价值可即时生效；明确现行实现若先生效后审为未对齐残余风险，本轮只定文档不改代码 | 用户确认 2026-07-27；对齐 PGM-0089 / dev-plan P0-C |
  + | 2026-07-28 | 增 | §10.2.1A | 出品-库存-赠酒-退单守恒式与抽检（docs-only；BAR-VOID-A1 配套） |

**本文件小计：** replace 块 3；仅备份条数 0；仅 main 条数 37；差异块 7

---

## 文件｜`dividend-engine-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/dividend-engine-prd-v0.1.md`
- main：`docs/dividend-engine-prd-v0.1.md`
- backup SHA-256：`4bedc43faf3f2eaec36e35db80911dd00263283021d0f88c74f58772191cd4f8`
- main SHA-256：`dc3367024ceff0e1d3b97455071da56476e96f4f5431ea791e784470c57e312f`
- 大小：24607 → 25526 bytes

- 对齐单元数：备份 172 / main 180

- 差异块数：2
- 相似度（序列比）：0.9773

### 差异项 1｜仅 main 有（main 新增）｜共 7 条

  + ## 4A. 跨周期维度补充（P2，2026-07-28）
  + 闭合 BSM-0411 `cross_period`（与 §4 期间作业一致，明示验收）：
  + 1. **正式周期 vs 预览**：默认季度正式 + 月度预览；切换为月度正式时，预览规则按方案配置，不得两套正式单重叠同一官方期间。
  + 2. **跨季/跨月重算**：已 `approved/paid` 的正式单不因后期费用调整自动重算；需冲销/作废后按权限重开期间作业。
  + 3. **关账锁**：正式非 preview 作业锁定费用侧期间后，跨周期补录费用不得无声进入已锁定分红基数；须走解锁/冲销策略（见既有 period-lock）。
  + 4. **比例/税改配置**：改方案只影响新期间；历史单按单内快照。
  + 5. **权限/证据**：跨期冲销、重算、解锁须最高权限或配置权限 + 全链路审计。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §4A | 跨周期维度明示（P2 BSM-0411） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 8；差异块 2

---

## 文件｜`finance-settlement-product-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/finance-settlement-product-prd-v0.1.md`
- main：`docs/finance-settlement-product-prd-v0.1.md`
- backup SHA-256：`afa63a811eb617189dd538efce60ccc66c1606091cbd7c34631be6cabe83997b`
- main SHA-256：`044fb7c8e7c93085966d74e344684264a42dd2848ab049e728d37eec2b3406c2`
- 大小：50745 → 54293 bytes

- 对齐单元数：备份 381 / main 425

- 差异块数：2
- 相似度（序列比）：0.9454

### 差异项 1｜仅 main 有（main 新增）｜共 43 条

  + ## 4A. 营业日与中国税法属期（双轨，2026-07-28 用户确认）
  + > 闭合审计 ADV-TX-008 / XD-006。
  + > **运营营业日**与**税法/开票属期**分开建模。
  + > 裁决：`DEC-BIZDAY-001`（运营）+ `DEC-TAX-CN-REFUND-001` / **T-INVOICE-LINK**（退款税会）。
  + > 本 PRD 不构成税务法律意见；落地以适用中国税法、主管税务机关及税控产品规则为准。
  + ### 4A.1 运营营业日（DEC-BIZDAY-001）
  + 用于：现场日结、班次、经营「今日」、低消/营收经营切片、商户经营对账默认视图（**非**税法定性）。
  + | 配置项 | 说明 | 可配 |
  + |---|---|---|
  + | 项目时区 | 如 `Asia/Shanghai` | 是 |
  + | 营业日切日时刻 | 项目本地时刻；默认建议 06:00，**禁止代码写死** | 是 |
  + - 订单/支付/关台等**运营事件**的 `biz_day`：按项目时区，落在切日点之后的营业日。
  + - 跨自然日 0 点但未过切日点：仍属上一营业日。
  + - 报表默认提供「按营业日」；「按自然日」为另一可选口径。
  + ### 4A.2 退款税会归属（DEC-TAX-CN-REFUND-001 = **T-INVOICE-LINK**）
  + 适用于：退款、撤销收款、已开票红冲/作废、需进入纳税申报的收入冲减等。
  + **总原则：中国税法与税局/税控现行规则优先；营业日切日不得替代法定/开票归属。**
  + #### T-INVOICE-LINK 规则（用户确认）
  + 1. **有原发票（已开票订单/流水）**
  + - 退款的税会/开票处理必须走合规 **红字发票 / 作废**（以票种与税控产品为准）链路；
  + - 红冲与冲减归属 **跟随原票与红字确认/开具流程**（及税控回传状态），不得仅按酒吧 `biz_day` 改写；
  + - 必须保留：原票号码、红字信息表/确认单号（若适用）、金额、购方信息、回传状态、操作人与时间。
  + 2. **无发票（从未开票或明确无需开票）**
  + - 税会归期按 **退款成功时点** 所适用的申报/入账规则处理（通常落入对应自然日/所属申报期）；
  + - **不**用运营营业日切日点替代该归期。
  + 3. **运营侧仍记 `biz_day`**
  + - 退款事件照常写入运营营业日，供日结与经营分析；
  + - 财务/结算界面必须能同时看到：`occurred_at`、`biz_day`、税会/开票属期或红冲状态；
  + - 当 `biz_day` 所属自然月与税会属期不一致时，展示差异提示，禁止静默抹平。
  + 4. **禁止**
  + - 已开票退款只做负向营收、不走红冲/作废；
  + - 用营业日切日把应属上期的红冲硬挪到下期（或反向）以「好看日结」。
  + 5. **可配项**（结构不可回退）
  + - 是否对接税控、红冲审批阈值、提示文案、无票退款的会计科目映射等可配；
  + - **不可配掉** T-INVOICE-LINK 与「税法优先」。
  + ### 4A.3 结算期
  + - 商户结算周期（T+N、月结等）可配，与 `biz_day`、税会属期在结算单上分列。
  + - 资金对账守恒以支付/分账流水为准；税会调整走调整凭证，不 silent 改写已锁日结资金事实。
  + ### 4A.4 验收
  + - 跨切日点支付：运营 `biz_day` 正确。
  + - 有票跨月退款：红冲链路完整；`biz_day` 与税会/开票状态可分离查询。
  + - 无票退款：归期不跟营业日切日绑定。
  + - 已开票退款缺红冲链路：阻断完成或进入「待税务处理」队列，不得当普通退款关单。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §4A | 营业日 + T-INVOICE-LINK 税法属期双轨（ADV-TX-008 用户确认） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 44；差异块 2

---

## 文件｜`full-admin-mvp-requirements-code-matrix-v0.1.csv`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`
- main：`docs/full-admin-mvp-requirements-code-matrix-v0.1.csv`
- backup SHA-256：`873625892809cb27c4619a4f5da23b480b9919fa1caa64336257307d778eeea0`
- main SHA-256：`ec71c699d908dec36b1b1aa3a229268e3166d835fabea3f17fbcde77acbc7fc2`
- 大小：3959 → 3967 bytes

- 对齐单元数：备份 10 / main 10

- 差异块数：1
- 相似度（序列比）：0.9000

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - "ADM-BAR-001","卡座与订台","卡座入场人数上限、转卡/合并/续卡审批提示、有效登记入场","/admin/bar/table-sessions","GET /api/v1/bar/tables; GET /api/v1/bar/reservations; POST /api/v1/bar/reservations/{id}/check-in-guest","apps/web/src/admin/pages/BarTableSessionsPage.tsx","tests/e2e/full-admin-mvp.spec.ts::ADM-MVP-04","已完整实现","7 人超限拦截，4 人登记 checked_in","P0"

**main 现行：**

  + "ADM-BAR-001","卡座与订台","卡座入场人数上限、转卡/合并审批提示（无续卡）、有效登记入场","/admin/bar/table-sessions","GET /api/v1/bar/tables; GET /api/v1/bar/reservations; POST /api/v1/bar/reservations/{id}/check-in-guest","apps/web/src/admin/pages/BarTableSessionsPage.tsx","tests/e2e/full-admin-mvp.spec.ts::ADM-MVP-04","已完整实现","7 人超限拦截，4 人登记 checked_in","P0"

**本文件小计：** replace 块 1；仅备份条数 0；仅 main 条数 0；差异块 1

---

## 文件｜`full-admin-page-permission-field-matrix-v0.1.csv`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-page-permission-field-matrix-v0.1.csv`
- main：`docs/full-admin-page-permission-field-matrix-v0.1.csv`
- backup SHA-256：`b2f4f175f769b86b84b79eca3765006ced4bb788539164791bdee7e37d2941e4`
- main SHA-256：`0bc3dfd3c02c659ee601d00a1bc2b87e130e89a2ae6c6c7fc561a9a64cb92b31`
- 大小：67746 → 67705 bytes

- 对齐单元数：备份 85 / main 85

- 差异块数：1
- 相似度（序列比）：0.9882

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - ADM-BAR-001,酒吧,卡座与订台,/admin/bar/table-sessions,MVP,SUPER_ADMIN;BAR_MANAGER;SALES,bar.table_session.view;bar.table.reserve;bar.table.transfer;bar.table.merge;bar.table.renew,GET/POST /api/v1/bar/tables;GET/POST /api/v1/bar/reservations;POST /api/v1/bar/reservations/{id}/check-in-guest;GET /api/v1/bar/orders,预订;转卡;合并;续卡;释放,bar_id;table_id;session_id;guest_limit;min_spend;sales_owner,reserved->seated->settled->closed/cancelled,免票人数超限拒绝;跨酒吧拒绝;未结账不可直接转卡,BAR_TABLE_RESERVED;BAR_TABLE_TRANSFERRED;BAR_TABLE_MERGED;BAR_TABLE_RENEWED,UI-ADM-BAR-001,已实现：完整后台首批 MVP 已接入正式 /api/v1 路径，详见 full-admin-mvp-requirements-code-matrix-v0.1.csv

**main 现行：**

  + ADM-BAR-001,酒吧,卡座与订台,/admin/bar/table-sessions,MVP,SUPER_ADMIN;BAR_MANAGER;SALES,bar.table_session.view;bar.table.reserve;bar.table.transfer;bar.table.merge,GET/POST /api/v1/bar/tables;GET/POST /api/v1/bar/reservations;POST /api/v1/bar/reservations/{id}/check-in-guest;GET /api/v1/bar/orders,预订;转卡;合并;释放,bar_id;table_id;session_id;guest_limit;min_spend;sales_owner,reserved->seated->settled->closed/cancelled,免票人数超限拒绝;跨酒吧拒绝;未结账不可直接转卡,BAR_TABLE_RESERVED;BAR_TABLE_TRANSFERRED;BAR_TABLE_MERGED,UI-ADM-BAR-001,已实现：完整后台首批 MVP 已接入正式 /api/v1 路径，详见 full-admin-mvp-requirements-code-matrix-v0.1.csv

**本文件小计：** replace 块 1；仅备份条数 0；仅 main 条数 0；差异块 1

---

## 文件｜`full-admin-ui-acceptance-matrix-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/full-admin-ui-acceptance-matrix-v0.1.md`
- main：`docs/full-admin-ui-acceptance-matrix-v0.1.md`
- backup SHA-256：`3f5cd004e7fd4d27e9c491c70b35dd95f71e1549de0de30ae08f2ac2736daa93`
- main SHA-256：`42e4ad0a01d8155ffe5ce61fe48371495b23ad64865efb120cc664a8508e0621`
- 大小：21324 → 21443 bytes

- 对齐单元数：备份 90 / main 91

- 差异块数：3
- 相似度（序列比）：0.9724

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | UI-ADM-BAR-001 | 卡座与订台 | 酒吧主管登录 | 预订卡座、转卡、合并、续卡、释放 | 人数限制、跨酒吧限制、未结账限制正确；审计链完整 | table sessions | 覆盖卡座权益不抵扣低消、跨酒吧拒绝 |

**main 现行：**

  + | UI-ADM-BAR-001 | 卡座与订台 | 酒吧主管登录 | 预订卡座、转卡、合并、释放（**无续卡**） | 人数限制、跨酒吧限制、未结账限制正确；审计链完整 | table sessions | 覆盖卡座权益不抵扣低消、跨酒吧拒绝 |

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 已建立完整后台首批截图基线：总控驾驶舱桌面、生产 blocker 与证据桌面、酒吧订单/出品/账单移动端、动态定价移动端。后续建议继续补权限模板复杂权限树、票务策略价格矩阵、卡座订台转卡/合并/续卡弹窗、应收应付台账、供应商适配器密钥脱敏与回调失败详情。

**main 现行：**

  + 已建立完整后台首批截图基线：总控驾驶舱桌面、生产 blocker 与证据桌面、酒吧订单/出品/账单移动端、动态定价移动端。后续建议继续补权限模板复杂权限树、票务策略价格矩阵、卡座订台转卡/合并弹窗（无续卡）、应收应付台账、供应商适配器密钥脱敏与回调失败详情。

### 差异项 3｜仅 main 有（main 新增）｜共 1 条

  + > A2 2026-07-28：移除续卡现行能力表述，对齐 DEC-SEAT-NO-RENEW-001 / bar PRD §7.3A。

**本文件小计：** replace 块 2；仅备份条数 0；仅 main 条数 1；差异块 3

---

## 文件｜`group-governance-rebuild-prd-v1.0.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/group-governance-rebuild-prd-v1.0.md`
- main：`docs/group-governance-rebuild-prd-v1.0.md`
- backup SHA-256：`e32f98b33b2ae20e8040eb548085a3a157a68143dc907aae75336de8f7fcf4aa`
- main SHA-256：`aade1ae6a91d7b5a1f3ad0489746ec8cd94dc1193a1f8886b64ec82c4f51751d`
- 大小：13179 → 12816 bytes

- 对齐单元数：备份 88 / main 90

- 差异块数：6
- 相似度（序列比）：0.9213

### 差异项 1｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - > 2026-07-04 逻辑一致性修订：①澄清角色分级——平台/集团级角色可跨项目公司下钻（requirePlatformScope 放行），项目公司管理员限本项目，商户用户 fail-closed 403，消除「集团下钻」与「项目公司级隔离」的表述冲突；②高危权限审批开关 `access.grant_approval.enabled` 据代码校正为**默认开**（原文「默认关」与代码不符）；③57 个高危权限码的 risk_level/requires_approval 改「后台可配、V170 预置不锁死」口径；④对标红点阈值据代码（V218）校正为「后台可配，默认 0.20；analytics_ai_thresholds」，非固定值。
  - > 生成：2026-06-27。依据：M1 需求采集 §7 + `platform-governance-map-for-operations-v0.1.html`「综合体和集团 / 权限规则」+ V169-V170 迁移 + `com.complex.platform.access` 本轮新增角色装配/授权审批 + `analytics/group-overview` 代码。本文向业务 HTML §十及治理 HTML 对齐；冲突以业务 HTML 为最终解释权，技术字段以真实 DB CHECK 和 Controller 为准。

**main 现行：**

  + > 2026-07-27 PRD 审计修订：①平台/集团级角色可按职位模板跨项目公司下钻，项目公司管理员限本项目，商户用户 fail-closed；②四类高危授权审批按治理 HTML 改为不可关闭的强制闸门，配置只能加严；③补授权申请人与审批人职责分离；④补岗位、职级、组织、商户或责任范围变化后立即重算并收回旧范围派生权限。当前实现若不满足这些规则，记为实现待对齐，不反向改写 PRD。
  + > 生成：2026-06-27。业务规则以三份业务 HTML 为最终解释权；迁移、代码和接口只作为当前实现证据，不得反向改写业务规则。本文承接治理 HTML「综合体和集团 / 权限规则」及枚举 HTML §九，M1 需求、V169-V170 和当前代码仅用于说明追溯与实现状态。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 高危权限审批开关字段（后台可配，逐权限码可改）；V170 预置把一批高危权限置为 `true`，数值可后台调整 |

**main 现行：**

  + | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 实现字段；四类 HTML 强制高危权限必须为 `true`，后台只能新增高危项或提高审批等级，不能关闭强制项 |

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - - 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；如果开启授权审批开关，含 `requires_approval=true` 权限的角色装配会被 `GRANT_REQUIRES_APPROVAL` 拦截，必须走 §3 申请。

**main 现行：**

  + - 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；角色装配只要包含四类强制高危权限或其他 `requires_approval=true` 权限，就必须被审批闸拦截并走 §3 申请，不受可关闭总开关影响。

### 差异项 4｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - - 审批总开关：`access.grant_approval.enabled` **默认开（`:true`，以代码 `AccessService` 默认值为准）**，后台可配。开启时含 `requires_approval=true` 权限的直接授权会被 `GRANT_REQUIRES_APPROVAL` 阻断，高危权限必须先走授权申请（§3 流程）；若后台关闭，直接授权仍按提权闸和跨组织闸执行，但不再因 `requires_approval=true` 自动阻断。（注：历史文档曾记「默认关」，与当前代码不符，2026-07-04 据代码校正。）
  - - V170 预置高危权限审批字段：按迁移 `high_risk` 清单（四类，预置 57 个权限码）对资金/核销/退款/调账类、提权或跨商户跨项目公司类、敏感数据类、高危业务操作类权限批量设置 `requires_approval=true`。**该清单为 P0 预置基线、非锁死：`permissions.risk_level` 与 `requires_approval` 均为后台可配字段，可逐权限码增删改（配置表 `permissions`）；V170 只给初值，不硬编码为不可变策略。**清单初值以 V170 迁移为准，不从业务说明另造权限数量。

**main 现行：**

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-13，本文不再重复记载。
  + - 高危权限配置：迁移清单只是当前实现初值。后台可以新增高危权限、提高风险等级或提高审批层级，但不得把 HTML 明确的四类强制项降级为无需审批，也不得移出强制范围。

### 差异项 5｜仅 main 有（main 新增）｜共 1 条

  + - 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。

### 差异项 6｜仅 main 有（main 新增）｜共 1 条

  + - 岗位、职级、所属组织、所属商户或责任范围变化时，立即按新归属重新计算有效权限；旧岗位和旧范围派生权限同步失效，员工单独授权若超出新范围则暂停并重新审批，不允许只提示人工收回。

**本文件小计：** replace 块 4；仅备份条数 0；仅 main 条数 2；差异块 6

---

## 文件｜`leasing-acquisition-prd-v1.0.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/leasing-acquisition-prd-v1.0.md`
- main：`docs/leasing-acquisition-prd-v1.0.md`
- backup SHA-256：`b75ab8fb0dc97ae4d4679da6c68aa3b183ce7e37000e1864ee08fe3af110ce6b`
- main SHA-256：`d0f0d4dcb97a8088c323867d29b3586211da59cf70fedceb71f1b52a42a27776`
- 大小：8725 → 10092 bytes

- 对齐单元数：备份 73 / main 84

- 差异块数：2
- 相似度（序列比）：0.9299

### 差异项 1｜仅 main 有（main 新增）｜共 10 条

  + ## 6A. 签约后到期 / 变更 / 退出衔接（P2，2026-07-28）
  + > 闭合 BSM-0267 `expiry_exit`。招商 PRD 负责「收到签约」；租约履约/退租细节以 real-estate / 合同履约 PRD 为准，本节定义**必须闭环的交接**。
  + 1. **意向/线索不会自动变成无限期占位**：签约转换成功后，线索/意向关闭为「已签约」类终态（名称以枚举为准），不得长期停在可编辑获客态。
  + 2. **合同到期前**：系统须能按可配提前期产生跟进/预警任务（到期提醒），避免静默过期。
  + 3. **到期路径（三选一，须留痕）**：
  + - **续约/变更**：走合同变更或新签流程，保留原合同与商户档案关联；
  + - **退租结算**：触发/转交退租与费用结清流程（租金、物业、能源、押金等以合同履约域为准）；
  + - **逾期未决**：到期后未续未退则进入「待处置」并限制新的超范围承诺（具体准入限制对齐合同/空间域）。
  + 4. **退出完成**：退租结算完成后，招商侧商户合作状态更新为退出/结束类状态，黑名单规则仍可拦截再入。
  + 5. **权限/证据**：到期处置、续约、退租发起均需授权角色；关键节点写审计事件与关联合同 ID。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §6A | 到期/变更/退出衔接（P2 BSM-0267） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 11；差异块 2

---

## 文件｜`operation-traceability-and-terminal-switch-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/operation-traceability-and-terminal-switch-prd-v0.1.md`
- main：`docs/operation-traceability-and-terminal-switch-prd-v0.1.md`
- backup SHA-256：`1a1ba173e77fc2b698092a99a14f8032b82a4efa48971ba123e40cfb9089ede0`
- main SHA-256：`c42caed8cfb184585f7c5d9051937fb142555b28dadc1dd00a3b66f74ec8c143`
- 大小：85171 → 85355 bytes

- 对齐单元数：备份 636 / main 637

- 差异块数：2
- 相似度（序列比）：0.9961

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 2. 所有终端都必须有开关接口，包括客人 App、小程序、Web 前端、工作人员平板 App、员工工作手机、员工私人手机 App、商户侧 App/Web、触摸屏一体机等，为后续酒吧系统和乐园系统独立 SaaS 化预留能力。

**main 现行：**

  + 2. 所有终端都必须有开关接口，十类终端分别为：客人 App、小程序、Web 前端、工作人员平板 App、员工工作手机、员工私人手机 App、商户侧 Web 门户、商户侧 App、触摸屏一体机、移动管理端。商户侧 Web 门户与商户侧 App 不得合并计数；开关为后续酒吧系统和乐园系统独立 SaaS 化预留能力。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 2 条

**备份原文：**

  - | 商户侧 App/Web | 控制商户侧入口和商户可用模块 |

**main 现行：**

  + | 商户侧 Web 门户 | 控制商户浏览器入口和可用模块 |
  + | 商户侧 App | 控制商户移动应用入口和可用模块 |

**本文件小计：** replace 块 2；仅备份条数 0；仅 main 条数 0；差异块 2

---

## 文件｜`platform-governance-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/platform-governance-prd-v0.1.md`
- main：`docs/platform-governance-prd-v0.1.md`
- backup SHA-256：`1f3ad60525131d36fd23283f862ddc5394c76f27d5d3fc2a6a7ac25581cc1e77`
- main SHA-256：`843f7d8d70294d6380eeba2d1e1564d334393c4edd7bdd658c86fde950eefe7e`
- 大小：58748 → 59930 bytes

- 对齐单元数：备份 416 / main 419

- 差异块数：8
- 相似度（序列比）：0.9772

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 2. 选择合作模式，如租赁、联营、自营、代运营、服务商接入。

**main 现行：**

  + 2. 选择合作模式：自营、租赁、联营、代运营、服务商接入或其他；选择“其他”必须填写说明。

### 差异项 2｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - 4. 提交财务或管理层做单级确认（P0 边界：仅"录入→确认→生效→终止"四态单级确认，不做多级审批流；多级/变更/续签审批流属 P1，见 §8.1）。
  - 5. 合同生效后，商户获得对应经营依据。

**main 现行：**

  + 4. 草稿提交后进入可配置审批流：默认业务、财务、法务同阶段并行会签，三方全部通过后进入总经理终审；任一节点驳回则整单退回起草。
  + 5. 审批全部通过后合同才生效，并固化租金、分成、物业费、能源费、保证金和违约金等关键规则快照；商户据此获得经营依据。

### 差异项 3｜仅 main 有（main 新增）｜共 1 条

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-13，本文不再重复记载。

### 差异项 4｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | 验收标准 | 平台角色和商户角色可区分；不同职级、角色和模板看到的数据范围不同；权限可与岗位/职级/薪资口径摘要关联；模板可复制并独立维护；临时模板到期或单次消耗后失效；敏感权限变更可追溯 |

**main 现行：**

  + | 验收标准 | 平台角色和商户角色可区分；不同职级、角色和模板看到的数据范围不同；显式拒绝优先于所有允许项；模板可复制并独立维护；临时模板到期或单次消耗后立即失效；岗位、职级、组织、商户或责任范围变化后立即重算并清除旧范围派生权限；敏感权限变更可追溯 |

### 差异项 5｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - - 合同审批流深化：支持多级审批、合同变更审批、续签提醒和到期预警。

**main 现行：**

  + - 合同审批流增强：在现有可配置分组并行会签与总经理终审基础上，增强审批分析、批量待办、续签提醒和到期预警。

### 差异项 6｜仅 main 有（main 新增）｜共 2 条

  + 14A. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务四类权限授出前必须先审批才生效；配置只允许新增高危项或提高审批等级，不得关闭这四类强制闸门。授权申请人与审批人必须职责分离，申请人不能审批自己的授权申请。
  + 14B. 岗位、职级、组织、商户或责任范围变化后必须立即重算有效权限并使旧范围派生权限失效；不得保留仅靠人工提示回收的残留权限。

### 差异项 7｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 18. 合同 P0 只支持"录入→确认→生效→终止"的单级确认流，不在 P0 做多级审批流；合同多级审批、变更审批、续签审批属 P1（见 §8.1）。> 修订说明（2026-06-07）：依 govern-5，二选一定为"P0 仅单级确认"，消除 §6.5/§7.5 与原 §12 待确认#5 的自相矛盾；原待确认#5 据此关闭（见下方待确认列表标注）。

**main 现行：**

  + 18. 合同采用可配置审批流：默认业务、财务、法务同阶段并行会签，全部通过后进入总经理终审；任一节点驳回则整单退回起草，审批全部通过后才生效。合同变更、续租、退租、面积或租金调整、经营范围变更和提前终止均保留审批、原因和版本快照。

### 差异项 8｜双方改写（replace）｜备份 2 条 → main 2 条

**备份原文：**

  - 3. 合同合作模式 P0 支持租赁、联营、自营、代运营、服务商接入五类；复杂组合模式通过合同摘要和结算规则配置承载，P1 再做复杂模板化。
  - 4. 合同 P0 仅"录入→确认→生效→终止"单级确认，多级审批流后置 P1。

**main 现行：**

  + 3. 合作模式固定为自营、租赁、联营、代运营、服务商接入、其他六类；“其他”用于前五类无法覆盖的特殊合作，必须备注说明。
  + 4. 合同审批按可配置分组并行会签执行，默认业务、财务、法务同阶段会签后由总经理终审；合同审批、变更、续租、退租和提前终止均须留痕。

**本文件小计：** replace 块 6；仅备份条数 0；仅 main 条数 3；差异块 8

---

## 文件｜`product-requirements-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/product-requirements-v0.1.md`
- main：`docs/product-requirements-v0.1.md`
- backup SHA-256：`2e57f18092b95415d89e685d33322984e7c39e7632381ad1b6f812500b8ec2b2`
- main SHA-256：`67b6b10f341f75fa58c7a2c7c5b29a20ff1c7602ad4e245c611f569a8d125959`
- 大小：43910 → 45406 bytes

- 对齐单元数：备份 444 / main 452

- 差异块数：3
- 相似度（序列比）：0.9888

### 差异项 1｜仅 main 有（main 新增）｜共 1 条

  + 业务事实以三份业务 HTML 为最终解释权；本文及其下游 PRD、接口、数据库、UI、测试和实现均须向 HTML 对齐，不能用当前实现反向解释业务规则。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - 34. 所有端都必须有开关接口，终端枚举以《统一口径基准》D5 十端为准：客人 App（`app`）、小程序（`miniapp`）、Web 前端（`web`）、工作人员平板（`tablet`）、员工工作手机（`staff_work_phone`）、员工私人手机 App（`staff_private_phone`）、商户侧 Web 门户（`merchant_portal`）、商户侧 App（`merchant_app`）、触摸屏一体机（`kiosk`）、移动管理端（`mobile_admin`）；开关支持按全局、综合体项目、商户、业务系统、模块、设备和版本控制，为酒吧/乐园独立 SaaS 化预留。<!-- 修订说明（2026-06-08 三轮收口）：原"App、小程序、Web、平板、员工端、商户侧和一体机"用 D5 已废笼统词（"员工端"未拆工作/私人手机、漏"移动管理端"、"商户侧"未拆 portal/app），改对齐 D5 十端权威码。依据 finding residual-scan-8、D5 -->

**main 现行：**

  + 34. 所有端都必须有开关接口，终端业务全集以治理 HTML「终端开关」为最终解释权：客人 App（`app`）、小程序（`miniapp`）、Web 前端（`web`）、工作人员平板（`tablet`）、员工工作手机（`staff_work_phone`）、员工私人手机 App（`staff_private_phone`）、商户侧 Web 门户（`merchant_portal`）、商户侧 App（`merchant_app`）、触摸屏一体机（`kiosk`）、移动管理端（`mobile_admin`）；开关支持按全局、综合体项目、商户、业务系统、模块、设备和版本控制，为酒吧/乐园独立 SaaS 化预留。D5 只承接技术编码映射，不反向定义业务全集。

### 差异项 3｜仅 main 有（main 新增）｜共 7 条

  + 40. 平台展示的所有数据、单据、对象及涉及人员和角色必须可点击追溯来源、归属和上下游，可正查反查，不允许孤岛数据。
  + 41. 全部用户可见界面、状态、枚举、按钮、提示、报错、导出、短信、通知和日期时间必须使用中文；只有机器识别的编码、ID、单据号等技术标识可保留原样。
  + 42. 金额计算、存储和显示统一精确到分、对客保留两位小数；分账、分摊或提成尾差由项目公司/平台承担并单列留痕，不并入任一参与方。
  + 43. 并发冲突以先处理成功者为准；同一操作重复提交只认一次，不得重复扣款、入账、核销或消耗权益。
  + 44. 存档用于日常下线，不需审批，历史仍可查并可恢复；删除必须先存档、填写删除理由、由另一人审批，申请人不得自审，执行后只隐藏不物理抹除，并保留查询、审计和恢复能力。删除前必须检查合同、账单、结算、订单、已发权益和库存等占用，存在占用时阻断。
  + 45. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务四类权限授出前必须先审批才生效；配置只能新增高危项或提高审批等级，不能关闭强制闸门，授权申请人不得审批自己的申请。
  + 46. 员工岗位、职级、所属组织、所属商户或责任范围变化后，必须立即按新归属重算有效权限并使旧范围派生权限失效；不得只提示人工收回。

**本文件小计：** replace 块 1；仅备份条数 0；仅 main 条数 8；差异块 3

---

## 文件｜`requirement-traceability-matrix-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/requirement-traceability-matrix-v0.1.md`
- main：`docs/requirement-traceability-matrix-v0.1.md`
- backup SHA-256：`82e66bd83df349cecd9bf424d01d6f28ce71dc5501799f844a456d2d36cbc1a9`
- main SHA-256：`a99e8855bd1716681c2e15f30ffe7d331afa227e33dd21b1c179638bb462e7e7`
- 大小：100879 → 99515 bytes

- 对齐单元数：备份 269 / main 269

- 差异块数：6
- 相似度（序列比）：0.9777

### 差异项 1｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > - **判定口径不变**：实现以 `backend/src/main/resources/db/migration/` + 代码为准；本横幅上修项已与 `matrix-migration-claims.json`（已置 present）一致。下方行内 R5 旧描述若与本横幅冲突，以本 R7 横幅为准。

**main 现行：**

  + > - **实现证据判定不变**：仅判断“当前是否已实现”时，以 `backend/src/main/resources/db/migration/` + 代码为证据；业务规则仍以三份业务 HTML 为最终解释权，代码不能反向覆盖。下方行内 R5 旧实现描述若与本 R7 横幅冲突，以本 R7 横幅为准。

### 差异项 2｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > - **REQ-35/42/68 留痕与准入权威事件（2026-07-03 用户裁决：文档对齐代码，非漂移）**：准入闸事件代码有意收口到 admission 域——发 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`（区分放行/驳回/告警/升级，较原 `contract.blocked`/`contract.released` 更细，`SupervisorReviewService` R5 T3 收口有据）；订单销售归属以字段 `service_responsibility_records.sales_staff_id`（V059）记录、可查询，属「记录型」留痕而非「事件型」，不发 `order.acquired`/`order.sales_attributed` 事件。→ **权威口径以代码实际为准，原「事件名漂移」结论作废。**

**main 现行：**

  + > - **REQ-35/42/68 留痕与准入实现事件名（2026-07-03 实现映射）**：业务 HTML 只要求阻断、放行、告警、升级和责任链可追溯，不规定技术事件码。当前实现使用 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`；订单销售归属以 `service_responsibility_records.sales_staff_id` 记录。上述代码和字段仅是实现映射，不反向解释业务规则。

### 差异项 3｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - > **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应；口径冲突一律以 `unified-standards-baseline-v0.1.md`（统一口径基准 D1–D13）与 `business-decision-log-v0.1.md` §32（2026-06-07 A1/A5/A6/A9 拍板）为准。

**main 现行：**

  + > **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应。原始需求、统一口径基准、决策日志和本矩阵均为追溯材料；业务冲突一律以三份业务 HTML 为最终解释权。

### 差异项 4｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`** + 4.1 准入事件 | 已覆盖（准入闸 V068 已落地）·事件名已对齐（有意 admission.* 收口） | R7 上修（2026-06-19，核验迁移/代码）：准入卡控框架已落（V068）——`AdmissionGuard` 经 `ActionGateAspect` 在带 `@GuardedAction(admissionAction=...)` 的业务方法执行前读 `supervisor_review_policies` 判定准入（三态 allow/warn/block），业务动作内嵌校验、卡控可触发并发 `admission.blocked/released/warned/escalated`；统一走 supervisor_review，admission_audits/contract_admission_rules 不建。**已对齐（2026-07-03 文档对齐代码）**：准入闸事件有意收口为 `admission.blocked`/`admission.released`/`admission.warned`/`admission.escalated`（区分放行/驳回/告警/升级，较原 `contract.blocked`/`contract.released` 更细，收口有据），权威口径改以代码 admission.* 为准；原「事件名漂移待对齐」结论作废。 |

**main 现行：**

  + | REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`** + 4.1 准入事件 | 已覆盖（准入闸 V068 已落地）·实现事件名使用 admission.* | 业务规则以 HTML 为准：准入阻断、升级、放行和结果必须留痕。当前实现以 `admission.blocked/released/warned/escalated` 承接；事件码只说明实现映射，不是业务事实源。 |

### 差异项 5｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-112 | **高危授权审批：requires_approval 激活、四类高危、SupervisorReview 三档审批、临时权限到期和智能总结** | group-governance-rebuild PRD §3；治理 HTML「临时权限/高危授权审批」；迁移 `V170__access_grant_requests.sql`；UI `ADM-ACC-003` / `UI-ADM-ACC-003`；E2E `AC-AUTH-06` | 已覆盖（V170 已落地，审批自动拦截开关默认关；P0 矩阵已补 UI/验收编号） | 新表 `access_grant_requests`；CHECK：`request_type`=`user_role/user_position/temporary_permission/role_permission`（服务当前支持前三类），`status`=`pending/applied/rejected`。端点：`POST/GET /api/v1/access/grant-requests`、`POST /grant-requests/{id}/review`、`GET /grant-requests/summary`、`POST /temporary-grants`、`POST /temporary-grants/{id}/revoke`。权限 `access.grant_request.view/apply/review`；配置 `access.grant_approval.enabled=false` 默认关；风险等级映射 `critical→permission_grant_l3/top_authority`、`high→l2/two_levels_up`、其他→`l1/one_level_up`；临时权限到期失效由 `AC-AUTH-06` 验收。 |

**main 现行：**

  + | REQ-112 | **高危授权审批：四类高危授权先审批后生效、主管三档审批、临时权限到期和智能总结** | group-governance-rebuild PRD §3；治理 HTML「临时权限/高危授权审批」；UI `ADM-ACC-003` / `UI-ADM-ACC-003`；E2E `AC-AUTH-06` | 业务规则已覆盖；当前实现如允许关闭四类高危审批，则为实现待对齐 | 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务四类授权必须先审批才生效，配置只能新增高危项或提高审批等级，不得关闭这四类强制闸门；申请人不得审批自己的授权申请。临时权限到期或单次使用后立即失效，授予、收回、到期、消耗和失效均留痕。 |

### 差异项 6｜双方改写（replace）｜备份 1 条 → main 1 条

**备份原文：**

  - | REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | unified-standards-baseline **D5**；operation-traceability PRD §5.2/§6.3；迁移 `V068` | 已覆盖 | 10 端权威表 terminal_type；govfam-3 跨文档枚举对齐以 D5 为准。R7 补（2026-06-19）：scope 7 维已落（V068 `terminal_capability_switches.scope_type` CHECK：global/project/merchant/business_system/module/device/version），`TerminalSwitchGuard` 按 device>version>module>business_system>merchant>project>global 解析，不再收窄 global/device。 |

**main 现行：**

  + | REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | 治理 HTML「终端开关」；operation-traceability PRD §5.2/§6.3；统一口径基准 D5 为下游编码映射 | 已覆盖 | 10 端业务全集以治理 HTML 为最终解释权；D5 和 `terminal_type` 仅承接编码映射。商户侧 Web 门户与商户侧 App 是两个独立终端，不得合并计数。 |

**本文件小计：** replace 块 6；仅备份条数 0；仅 main 条数 0；差异块 6

---

## 文件｜`terminal-page-product-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/terminal-page-product-prd-v0.1.md`
- main：`docs/terminal-page-product-prd-v0.1.md`
- backup SHA-256：`44d7d747c2882565305b1507d2d5422a4a38993fa495fd5c51ad3e4d820a5d7c`
- main SHA-256：`810d67e6935c39886a22b5c913859cc57a6bfb6b4af5f405bdf804a7551fad7c`
- 大小：39191 → 41861 bytes

- 对齐单元数：备份 295 / main 308

- 差异块数：2
- 相似度（序列比）：0.9784

### 差异项 1｜仅 main 有（main 新增）｜共 12 条

  + ### 11.1 业务 HTML 权威规则承接（docs-only 补齐，2026-07-28）
  + > 最终解释权：`business-system-map-for-operations-v0.1.html` / `platform-governance-map-for-operations-v0.1.html`。下列条款为对权威 HTML 的可验收正向/逆向表述；与 HTML 冲突时以 HTML 为准。
  + | 权威规则 | 正向 | 逆向/拒绝 | 权限与证据 |
  + |---|---|---|---|
  + | BSM-0130 一体机定位 | 一体机是固定在吧台、权益领取点、游乐服务点或授权售票点的触摸屏；供客人或现场人员完成查询、权益领取和部分下单 | 不得把一体机当作可随意挪动的普通展示屏或个人作业手机；未绑定点位/设备编号/业务模块开关的设备不得进入业务态 | 设备绑定点位、设备编号、模块开关、事件留痕 |
  + | BSM-0132 售票型一体机售卖范围 | 仅**启用售票能力**的一体机，可由**授权服务人员登录后**辅助客人购票；在某酒吧门口/前台售票时，**只能售卖该酒吧单店门票** | **禁止**售卖综合体通票；未启用售票能力的一体机禁止进入辅助购票；未授权人员禁止登录售票 | 点位所属酒吧、票种范围校验、操作人权限、拒售原因留痕 |
  + | BSM-0135 支付引导 | 支付优先生成二维码由客人手机扫码支付；也可由工作人员辅助扫描客人付款码 | 不得绕过账单直接「口头收钱不入账」；现金/POS 必须进入账单与对账 | 支付方式、经办人、账单/日结关联 |
  + | BSM-0142 权益扫码领取 | 一体机连接票务权益：客人扫码后展示可领取权益，领取确认后回写权益状态 | 不可领取/已领取/过期必须明确拒绝并展示原因；禁止只展示不回写 | 权益状态机迁移、核销/领取证据 |
  + | PGM-0075 工作手机职责 | 工作手机负责扫码核销门票和权益（及库房交接等现场扫码） | 工作手机**不是**酒吧点酒主入口，也不是后台配置入口 | 设备角色=工作手机、核销成功/拒绝记录 |
  + | PGM-0077 平板职责 | 工作人员平板**只负责**酒吧点酒下单（及卡座账单/低消相关现场动作） | 平板**不得**代客购买门票，不是后台配置入口 | 设备角色=工作人员平板；试图购票必须拒绝 |
  + ### 11.2 游乐准入冲突（指针）
  + 游乐多限制冲突与监护按 `amusement-business-operations-product-prd-v0.1.md` §7.2A（**固定优先级链，只报首条失败**，DEC-ADM-PRIORITY-001）。终端不得自创并行规则。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §11.1 | 补 HTML 权威规则 BSM-0130/0132/0135/0142、PGM-0075/0077 可验收承接（docs-only deferred 清偿） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 13；差异块 2

---

## 文件｜`ticketing-entitlement-checkin-product-prd-v0.1.md`

- 备份：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/ticketing-entitlement-checkin-product-prd-v0.1.md`
- main：`docs/ticketing-entitlement-checkin-product-prd-v0.1.md`
- backup SHA-256：`c60e9d4209ccf3a5d7b1e26aad622ce9c922b89039bec3c3d2de5c951094ab67`
- main SHA-256：`3f6e995a52a2e679fa05d5f53ff8c59323352c84c95ebfbde80cd17ca24aa7f5`
- 大小：35803 → 37502 bytes

- 对齐单元数：备份 338 / main 349

- 差异块数：2
- 相似度（序列比）：0.9840

### 差异项 1｜仅 main 有（main 新增）｜共 10 条

  + ## 12B. 跨结算周期维度（P2 文档闭合，2026-07-28）
  + > 闭合覆盖缺口：`cross_period`（BEQ-0014/0017、BSM-0042/0045/0059 等）。不改 HTML 真值；与 `finance-settlement` §4A 营业日/税法双轨一致。
  + ### 12B.1 通票/权益结算跨周期
  + 1. **核销才结算**：通票赠酒、通票餐饮优惠、组合通票分商户权益等，均 **按实际核销逐笔** 进入应付/结算，不因「购票落在上期」而在购票期预结全额。
  + 2. **未核销跨期**：权益有效期内跨自然月/结算期仍可核销；**未核销部分票款留存规则** 以 HTML「未核销归项目公司留存、与权益归属分开」为准，不因跨期自动改归属。
  + 3. **过期未核销**：权益过期后不得再核销、不得再对承接商户产生新的核销结算；已核销历史保留。
  + 4. **跨期退款/冲正**：运营 `biz_day` 记退款发生营业日；资金与商户结算冲回按结算单规则；涉及开票/税属期走 `T-INVOICE-LINK`（finance §4A），**不得**用营业日切日改税期。
  + 5. **综合服务费跨月封顶**：封顶按「项目×商户×票种」**自然月或可配会计月**累计（默认自然月，可配）；跨月计数清零重计；已收取不因次月调价回算，除非单独调账审批。
  + 6. **联合通票多商户**：各权益核销落在各自商户与各自结算周期；同一张票的不同权益可落入不同结算期。
  + 7. **权限/证据**：跨期调账、手工补结算须授权+原因+关联原核销/订单；结算单与流水可按期间筛选对账。

### 差异项 2｜仅 main 有（main 新增）｜共 1 条

  + | 2026-07-28 | 增 | §12B | 通票/权益/服务费跨结算周期维度（P2 PARTIAL 闭合） |

**本文件小计：** replace 块 0；仅备份条数 0；仅 main 条数 11；差异块 2

---

# 三、使用说明

1. 你要对比的「每一句话」在对应「差异项」下以 `-`/`+` 列出。
2. replace 块请整段对照：先读备份 `-`，再读 main `+`。
3. 若某条你认为备份更对，点名文件 + 差异项编号，我们再决定是否回灌 main（积分/会员除外）。
4. 未列出的 IDENTICAL 文件：备份与 main 字节级相同，无差异句。
