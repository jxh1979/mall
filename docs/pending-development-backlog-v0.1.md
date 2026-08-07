# 待开发 / 未完成项 登记 v0.1（带优先级）

> **口径修订（2026-08-06 R10）：票务「核销预检」两步流已废止**——ENUM「核销记录状态」条 明确核销记录状态**共六态**（成功／拒绝（规则不允许）／失败（设备或网络原因）／已冲正／待复核／异常）、「**不设「预检通过」态**」，且「**检票环节内部为一次动作：扫码即核销，不设先预检、后确认的两次操作**」（DEC-R6-08 / 第三轮用户裁决 UD-11）。本文原有的 `preview_passed` 状态、`verification.previewed` 事件、`/verifications/preview` 预检端点与「预检 → 确认」两步核销流程**一律废止**，废止处就地标注；已实现的端点、事件与取值待代码轮下线，期间不得新增调用、不得新写入。**限定**：本条只针对票务核销域的预检；酒吧年龄/人脸核验预检、运行配置预检、分红预检等其他域的「预检」不受影响。

> 创建：2026-06-30。**2026-07-28 全量回写（当前有效）：见第 0b 节，旧第 1–3 节仅存档。**
> ⚠️ 上一版自称「2026-07-05 全量刷新」，此后 125 个迁移（V232 → V357）无人回写，已导致有人照着它立项、做到一半才发现早就做完了。**引用本表前先看 §0b 抬头的核查基准与证据列。**
> 基于全仓只读盘点（文档待办标记 + 后端代码 TODO 扫描[近乎为零] + 功能开关 + 记忆跟踪项）。
> 口径：第三方/支付/人脸类「预留接口+开关默认关」属**设计性延后**（用户拍板、等真实资料/开通），不计为"欠功能"，单列第 4 节。真实净新待开发集中在第 1–3 节。
> 最终口径仍以三份业务 HTML + `00-current-effective-document-entry-v0.1.md` 为准。

## 0. 完成状态（2026-06-30 本轮收口）

P1/P2/P3 已依次逐项完成并测试闭环（提交见 git log）：
- **P1 全部完成**：①外包供应商自助下属门户（partner 包+5端点+ADM-VND-004 页+7测试，b8674a5）②业务短信通知（触发点已落地，补 ADM-INT-004 两级开关页，559d596）③全链可点击下钻基建 TraceLink+navigateTo（899cfca）
- **P2 全部完成/核实**：④下钻 P2 应用 project/space/merchant（b09a66f）⑤租赁状态看板 ADM-LEASE-002（ca2c072）⑥楼宇深化经核实多数已落地、剩 IoT/远程表硬件门控+分摊/排程增强（回写 3e25ec0/bac8fed）⑦酒吧退菜退酒/反结/pour 采集经核实已实现+测试（回写 edb69fe）
- **P3 全部完成**：⑧下钻 P3 应用 contract/merchant（283d90c）⑨Playwright ADM-REM-01 实跑通过（21.8s，覆盖全部新增页）
- 下钻基建 TraceLink 已就绪，剩余装饰性引用按同一组件逐页替换为机械收尾，不阻塞。

> 下表为本轮开工前的原始登记，保留作对照；实际完成状态以本节为准。

## 0b. 待办总表（**2026-07-28 全量回写，当前有效**）

> ⚠️ **本节 2026-07-28 按实读核查逐条回写过一遍。** 上一版自称「2026-07-05 全量刷新」，但此后 125 个迁移
> （V232 → V357）无人回写，已经把人坑过一次：有代理照着本表立项，实读后发现要做的东西早在 07-05 当天就做完了。
> **看本表前先看这句**：每条状态后面都带了证据（迁移号 / 提交 / 端点 / 页面），标 ✅ 的请先按证据核一眼再说要重做。
>
> - 核查基准：main = `4fb84be1`（2026-07-28）。核查报告：`.160:~/backlog-truth-sweep-20260728.md`。
> - **迁移号已用到 V357**（原写 V232，差 125 个）。
> - **2026-08-01 S10 增量刷新（仅动结算重建相关行）**：main = `ce76f1ad`，**迁移头 V375**；分账瀑布重建 S1–S11 全部合入（对照表见 `settlement-rebuild-prd-v1.0.md` §14.2），本表批 11 收口、新增「分账瀑布收官遗留」行。其余行维持 07-28 基准未复核。
> - **2026-08-03 增量刷新（仅新增 I 节，未复核旧行）**：main = `e8f7fef7`，**迁移头 V385**；本轮为 Codex 双轨审计核验后的修复批，已修 14 项见 I 节下方「本轮实际动了什么」。
> - 后台已注册页 **84 个**（`apps/web/src/admin/adminRoutes.ts`）——凡引用页号的待办，先核这张表里有没有这个页号，
>   本轮就查出 3 个根本不存在的页号（见 A6）。

### A. 前端体验线

| 项 | 内容 | 状态 |
|---|---|---|
| A1 KPI 卡全站铺开（38 页） | 英文 `<small>` 子标签→中文 + KPI 数字可点下钻 | ✅ 已完成（main） |
| A1b 全链可点击 P1/P2（引用下钻） | 引用字段全站可点下钻（统一 `<TraceLink>`）；P2 商户门户内链+来源合同（43d9bae） | ✅ 已完成（main） |
| A1c 整行可点→右侧明细抽屉 | ~60 个数据表页整行可点→ `RowDetailDrawer` | ✅ 已完成（main） |
| A1d 全站零英文 | 枚举/状态/下拉/表格/ISO 时间/表头/后端 CSV 导出全中文 | ✅ 已完成（main） |
| A1e 原生日期控件→中文选择器 | 自研 `CnDatePicker`（11 页 18 处，7502a08） | ✅ 已完成（main） |
| A1f 会话过期回登录页 | 后端统一 401+`AUTH_SESSION_INVALID`；前端 401→静默 refresh→失败回登录页（951f457） | ✅ 已完成（main） |
| A1g 布局根因修 | `.admin-shell` grid 行封顶 + KPI 聚焦滚动指真实容器 | ✅ 已完成（main） |
| A1h 字段错位/视觉修 | 组织商户/拓扑/准入读真实字段 + 表单溢出等 | ✅ 已完成（main） |
| A2 全链可点击 P3（域内引用） | `TraceLink` 已接 30/84 页。**剩余 `equipmentId`/`ticketTypeId` 命中基本都是表单输入框和 state 字段，本来就不该做下钻**；真正的展示型引用只剩零星几处（如 `FacilityWorkbenchPage.tsx:377`「关联设备」明细行） | ⏸ **不建议单独立项**（价值极低，随手改） |
| A3 审计反查入口 | `AuditTrailHint`（内含 `AUDIT_PAGE_ID="ADM-EVD-001"`）已接 **48/84 页（57%）**。原写「是否全站到位待核」→ 答案是**未全站到位、但远非未做**。未接的 36 页里配置页/纯分析页很多本来就没有审计对象 | 🔶 部分完成。**下一步先花 0.5 天出「该有却没有」的名单，不要直接按 36 页派活** |
| A4 高亮类统一 | `admin-row-focus`（3 页）与 `admin-row-selected`（11 页）并存 → 统一保留 `admin-row-selected`，删掉另一个 | ✅ **已完成（2026-07-28，本轮）** |
| A5 下钻验收纳入矩阵 | 「点击引用→落目标页并定位」写进 `full-admin-ui-acceptance-matrix` §1 原则第 7 条 + 新增 §3.6（三条 G/W/T + 统一断言口径，指向 5 个现成样板测试）+ §5 不得上线条件 | ✅ **已完成（2026-07-28，本轮）** |
| ~~A6 full-admin-matrix 4 页补 Playwright~~ | 🚫 **整条作废（2026-07-28 实读）**——不要按「补 4 页 e2e」派活，页号是错的：<br>· **ADM-CONT-003 / ADM-FAC-003 / ADM-ENE-003 三个页号在 `adminRoutes.ts` 里根本不存在**，是 2026-06-27 旧待办遗留的过期页号（能耗已改由 ADM-ANA-007「能耗分析」承载，且 `tests/e2e/full-admin-remaining.spec.ts:75` 已覆盖）<br>· **ADM-ACC-003「访问治理工作台」确实存在，且已在 `full-admin-remaining.spec.ts` 中被覆盖** | 🚫 作废。**替代动作**：先核对 84 个注册页与 e2e 实际覆盖的差集，按差集出新清单再派活 |

### A'. 时间属性 / 履约时间戳（已落地）

| 项 | 内容 | 状态 |
|---|---|---|
| A'1 下单业务记支付时间 paid_at | **V231**：票务/酒吧/游乐/储值补 `paid_at`（e4f751a） | ✅ 已完成（main） |
| A'2 物流配送到位时间 | **V232**：`bar_order_items` 补 `delivered_at`；游乐核销时间+场次展示（433f9af） | ✅ 已完成（main） |

### A''. 游乐核销走正路（三阶段全完成）

| 阶段 | 内容 | 状态 |
|---|---|---|
| 阶段 1 通票游乐权益核销分流到游乐正路 | 定位项目 + 当前场次 + 扣容量 + 记项目/场次/时间 | ✅ 已完成（38fedee6） |
| 阶段 2 出场核销 + 主管释放 | **V234**；自由进出型并发容量回收、主管释放须填理由落 override 留痕 | ✅ 已完成（6e550ec5） |
| 阶段 3 前端游乐核销记录/场次展示 | ADM-AMU-005 三视图 + `/verifications`、`/capacity-snapshots` | ✅ 已完成（5db50802） |

### A'''. 客人端（H5）

| 项 | 内容 | 状态 |
|---|---|---|
| A'''1 客人端零英文/可点击 | **零英文 = ✅ 已达成**：10 个客人端页面 grep JSX 文本节点纯英文串（`>Xxxx<` 形态）**零命中**，铁律已满足。<br>**可点击 = 不适用**：客人端是「我的订单→我的票→出码」的线性移动端流程，后台那套「引用字段下钻到源业务页」在客人端没有对应业务场景（客人不需要点合同号去查合同） | ✅ **消项**（原「⬜ 待核」） |

### B. 结算重构剩余批次

> **批 1–4 已落地**（V222–V232）。迁移头 **V375**（2026-08-01；「已用到 V357」为 07-28 时点值）。

| 批 | 内容 | 状态 |
|---|---|---|
| 批 5 配置中心 | **主体已上线**：ADM-STL-001「结算配置中心」页真实存在（`SettlementConfigPage.tsx`，antd 三页签），**10 个配置块**均可视化编辑并走「草案→他人复核转正」；迁移 V326/V327/V328/V329/V330/V331/V336/V337/V340/V343；后端 `SettlementConfigWriteService` + `JdbcSettlementConfigWriteRepository` 统一草案写入面。<br>**两项已作废、不再是缺口**：①「赠酒成本 `giveaway_cost_configs`」——D5 拍板赠酒不占分账池（`TicketDistributionSettlementService:206` giveaway_cost_cent 恒 0），属**消项**；②「税费 `settlement_tax_configs`」——当前无消费方（`JdbcSettlementConfigWriteRepository:404` 注释「零消费不入」），属**设计性搁置**，等批 4b 开票清单立项才有用。<br>**3 个尾项已于 2026-07-28 补齐（PR #274，零迁移）**：<br>　① ✅ **人脸照 `require_face_photo` 模板勾选**——已进权益模板**新建**表单，列表/详情一并展示。（既有模板改此开关仍缺编辑页，归本批主体）<br>　② ✅ **票种分类 `ticket_category` 建票入口**——建票向导加「票种分类」下拉（标准/新人/学生），随建票载荷下发<br>　③ ✅ **虚拟权益签约价 `agreement_price_cent` 改价入口**——新增 `PUT /api/v1/ticketing/entitlement-templates/{id}/agreement-price`（`ticket.entitlement.manage`·幂等·审计 `agreement_price_changed`）。**不是裸 PUT**：三道闸（已存档/待删除拒改、仅 `virtual` 适用、价必填非负 + 理由必填不接受留空清价）＋ **只对之后发放的权益生效**——签约价发放时快照进 `issued_entitlements`、核销时再快照进 `ticket_verifications.settlement_price_cent`，已发放/已核销的钱改不动，「已发生的不可改」由快照链结构性保证（证伪测试 `SettlementPriceResolutionJdbcIntegrationTests#agreementPriceChangeDoesNotMoveAlreadyIssuedOrVerifiedSettlementMoney`）。<br>　**与批 10 的边界（2026-08-01 已兑现）**：批 10 落地时本端点如约**下线**——`PUT …/agreement-price` 已删（openapi 714 操作），改价唯一入口收敛为目录草案链（落草案→他人复核转正→快照传播），模板上的 `agreement_price_cent` 转为目录价快照只读。<br>**2026-08-01 S10 注（分账重建对本批的覆盖）**：「项目公司留存比例」卡片已随 S4/V370 **整体下线**（留存并入综合服务费）；新增「赠送权益预估成本」配置块（S6，`pass_gift_cost_configs`）；「各酒吧分配方式」卡片改两段口径、`method_type` 仅 `by_rank_tier`（S8/V374）——本批「剩主体消项梳理」不再包含这三处 | 🔶 **部分完成**，3 尾项已清，剩主体消项梳理 |
| 批 6a 渠道费数据源 | **✅ 已完成**，原写「现暂置 0」是过期描述：V327 建 `channel_fee_configs`（开发库实存 1 条配置）；`TicketDistributionSettlementService:107-115` 注释直书「率可配，**消灭写死常量**」，真查库、查不到走 `requireConfigured` 报错而非静默 0；后台可改（结算配置中心第 7 块「渠道抽成费率」） | ✅ **消项** |
| 批 6b 第三方通票适配 | **✅ 已完成（V382 · 2026-08-01）**：`IntegrationOrderSyncService` 落 ticket 分支——渠道回流订单经 `integration_channel_ticket_type_map`（V382，第三方商品 id → 本地票种，可挂渠道团购价规则）解析后走**既有出票主链**（下单 sales_channel=具体渠道 → 支付意图 → 支付确认 → 出票，权益拆分/票码防伪/分摊快照/S3 父票/S11 分项价全复用），同渠道订单号幂等只出一次票；结算 S11 渠道费按 `ticket_orders.sales_channel` 命中该渠道费率（V327/V381 既有解析，零改动直接生效）。核销/冲正回传落 `integration_retry_tasks` 出站队列（`verification_callback`，真实适配器未接入前待发可见可重试）。channel-auto 对账 ticket 系统取数（平台侧期望=本地票订单实收直读真源）。能力开关 `integration.ticket_channel.sync`（V071 种子）**默认关**，关=回流拒收「渠道未启用」；映射缺失 fail-closed 拒收。后台页 ADM-INT-005「渠道映射维护」（票种+门店两张翻译表）。**退款回流有意只落台账进对账**：本地退票退款必须走既有退款审批链（先审后行），不因渠道回流一条退款记录自动退。真实供应商适配器（验签/字段映射/回传出站执行器）等厂商资料，接入时打开开关即可 | ✅ **消项** |
| 批 4b 开票清单 | **✅ 已完成（V362 · ADM-STL-002 · 2026-07-28）**：PRD 附录 B.1 两表已落 —— `invoice_subject_rules`（开票主体×收入类型 → 发票项目名称/税目口径/凭证要件/是否对外开票，种子=底稿 23 逐行）+ `settlement_invoice_items`（开票清单条目，开票状态六态照业务 HTML）；后台页落 **ADM-STL-002**（原写 ADM-STL-008，按实际页号收敛），按开票主体分组 + 税率税额 + 导出 CSV。<br>**不新建第三张配置表**：主体解析复用 `revenue_stream_attribution`（V319/V355），税率复用 `settlement_tax_configs`（V223 已按附录 A·C 全量种子，**批 5 里写作「零消费、设计性搁置」的这张表由本批接上消费方**）。<br>**金额铁律**：可开额 = 应开额（结算单当期净额）− 已开额（待开票/开票中/已开票 三态合计），≤0 不生成 —— 一式给到重复生成幂等、红冲后额度释放即「重开」、退款超开时汇总报警提示先红冲。<br>**未做（有意）**：向商户供货货款（中央餐饮）暂无结算单来源不出条目；非散台 / 餐饮 / 租金 / 游乐 / 演出 五类规则种子已就位，各自结算路径上线时接同一生成器即出条目 | ✅ **消项** |
| 批 7 名词四层文档同步 | **✅ 已完成**：权威表 `terminology-canonical-v1.0.md` 07-28 两次同步（95834281 / 10515d53）+ V319/V347 落库；本轮（2026-07-28）清掉下游 3 份文档 12 处废弃名词（`settlement-rebuild-prd` 6 处、`prd-audit/HTML-PASS-…` 1 处、`prd-audit/authority-rule-ledger.csv` 5 行按现行 HTML 重抽）。<br>纠正表「旧词」列、CONTEXT.md 废止别名、改名记录、V319 冻结历史、V347 改名迁移**有意保留**，删掉等于抹掉改名审计链 | ✅ **消项** |
| 批 8 租售比（业态级+商户级） | **✅ 已完成**：`AnalyticsService.rentToSales(month)`（注释「P1c 店级/业态级租售比，2026-07-21 拍板」）+ `JdbcAnalyticsRepository.rentToSales` + `AnalyticsController:167` 暴露 `/api/v1/analytics/rent-to-sales`；两个前置条件都已落（商户业态字段 = **V348** `merchants.merchant_type` CHECK（**V384** 起六值，加传媒）+ 按业态筛；`integration_order_syncs` 补 merchant_id = **V334**）；前端 `OccupancyAnalyticsPage.tsx:219-260` 三级视图 + **未归属营业额单列不静默摊给商户**；营业额≤0 时 ratioBps=null 标「不可计」不伪造 0。<br>**唯一遗留是运营数据不是代码**：开发库 `integration_channel_store_map` = 0 行，映射表还没人录，店级分母目前全落「未归属」 | ✅ **消项**（剩录数据的操作事项） |
| 批 9 持股分红 | **✅ 已完成**：后端独立包 `backend/.../dividend/`（20 文件：Controller/Service/Repository/DividendMath/DividendPeriodLock/**DividendMonthlyPreviewJob**/DividendReserveReleaseJob/3 dto）；迁移 6 支 **V299**（净利润制+真股东 20% 代扣）/**V302**（期间关账锁+月度预览作业）/**V303**（扣除项三开关）/**V304**（发放保护线留存·释放）/**V308**（本金类型）/**V309**（自定义扣除项）；开发库 5 表实存；前端 ADM-FIN-007 + ADM-MER-008 均有 guidedcreate 测试；PR #137/#143/#149/#153/#165。<br>原写的「平台 80%/主理人 20% 可配、周期作业」两点均已实现；「成本口径边界与财务对齐」是流程事项非代码缺口 | ✅ **消项** |
| 批 3-5 综合端到端（可选） | 一张通票同时含实物+虚拟+场次+当天无限+专属的全链交互验证（各片已各自充分测） | ⬜ 可选 |
| **批 10 内部结算 SKU 目录** | 每个项目一个目录、本项目成本侧唯一真源（范围经 DEC-MPS-2026-07-28-15 重裁为**按项目**，落地为「条目商户=提供商户」商户级同源守卫）；**目录是唯一可编辑处**，改价走草案→他人复核转正→对后续核销/结算生效；核销/结算固化当时价到流水、固化价只读；仅授权后台可见。**✅ 2026-08-01 落地（V379/V380）**：`internal_price_catalog`（Path 2 第 12 类，转正+四表快照传播同事务）；一价四存全部收编为 `catalog_item_code` 引用+快照只读、四处写入口封写；解析器两支改读目录；目录主页面 ADM-STL-003；收编等价与「历史不受改价影响」均有证伪测试。权威：`finance-settlement-product-prd-v0.1.md` §3B | ✅ **已落地** |
| **分账瀑布收官遗留（2026-08-01 S10 新增）** | S1–S11 收官后仍未落地的尾巴，集中一行防散失：① ~~差额调账留痕~~ ✅ 已落地（2026-08-01 S7：**V377** `pass_gift_cost_variance_ledger` append-only 差额台账，月结按「项目×规则」结 V=G−A（DB CHECK 恒等），三处理规则 platform_absorb/carry_forward/true_up_pool 全实装，结算审阅页补「赠付差额」四列）；② ~~兑付成本按商户业态分记~~ ✅ 已落地（同 V377：`settlement_bill_format_shares.gift_actual_paid_cent` 按提供方业态分记，K14 Σ各业态实付≡台账实付）——①② 即原 S7 范围，详见 `settlement-rebuild-prd-v1.0.md` §5.3/§10.2E/§14.2；③ ~~免费权益成本承担方「未标明不得启用」必填强校验~~ ✅ 已落地（2026-08-01 用户拍板：必填、漏标拒保存/拒发布，**V376** 撤默认撤 NOT NULL、赠送类漏标 `TICKET_COST_BEARER_REQUIRED` 点名拒绝、前端必选下拉，谓词与 S5 `providerRedeemed` 同源不误伤非赠送类）；④ ~~渠道费固定金额模式~~ ✅ 已落地（**V381** `fixed_amount_cent` + `chk_cfc_fee_mode` 比例/固定金额二选一；⑤ ~~超池特批入口 UI + OpenAPI 契约收编~~ ✅ 已合入（PR #287，2026-08-01）；⑥ G 层过账随过账总闸激活批次（`settlement.monthly.posting.enabled` 保持关） | ⬜ 待排期（⑥ 余,随过账总闸批次） |
| **批 11 综合／联合通票分账 + 两段分配 + 免费权益成本承担** | 2026-07-28 拍板 A/B/C/D（DEC-ORG-2026-07-28-08~11），承接 `settlement-rebuild-prd-v1.0.md` §5.0c/§5.1/§6.6：① 业态份额拆分层 ✅ S11（V366，PR #277）；② 各业态价格（原称「票面原价」，2026-08-05 DEC-R7-16 更名）`component_price_cent` + K21 价线防护 ✅（S11/V369）；③ 两段分配 + 守恒两段化 ✅ S8（V374/V375，PR #285）；④ 成本承担方 `cost_bearer` + fail-closed 取价 ✅ S5/S6（V372，PR #270/#286）。整轮 S1–S11 对照表见 `settlement-rebuild-prd-v1.0.md` §14.2 | ✅ **消项（2026-08-01 S10 收官）**；残余尾巴移「分账瀑布收官遗留」行 |

### C. 游乐票人脸照配套前端（后端 V229 已落）

> ⚠️ **实读复核纠正（2026-07-28，V359）**：C1 原写「base64 入出票请求 `facePhotos`」不成立——
> 本仓出票只有后台一条路（`POST /ticketing/orders/{order_id}/issue`，权限 `ticket.ticket.issue`），
> 客人端根本调不到；客人买票时权益还没发放，脸照也无处可挂（V229 的存档表按 `entitlement_id` 唯一）。
> 因此 C1 不是纯前端活。落法：客人在 H5 拍照 → 照片随下单请求上来 → 加密**暂存到订单**
> （V359 给 `ticket_face_photos` 加订单位 `order_id`+`slot_index`，同一张表同一套加密与清理作业）→
> 后台出票时自动取用、转绑到已发权益、立刻删暂存。出票请求自带照片（柜台现场拍）仍优先，口径不变。

| 项 | 内容 | 状态 |
|---|---|---|
| C1 H5 购票拍照 | 客人端购票页对「要拍照的票种」告知用途与留存天数、取得同意后调摄像头逐张拍；base64 随下单请求上来加密暂存到订单，后台出票时转绑到已发权益并删暂存 | ✅ 已落地（V359） |
| C1b 拍照同意落库留痕 | **V368（用户 2026-07-28 裁决「要，落到同意台账」）**：V359 的同意只在前端勾一下、库里不存，被问「客人同意过吗」拿不出证据。落法：下单时往**既有**同意台账 `customer_consents` 落一条（`consent_type='ticket_face_photo'`、`version=告知模板版本@当时告知的留存天数`、`accepted_at`、`source_terminal='miniapp'`），**不新建第二套台账**；照片行 `ticket_face_photos.consent_id` 挂这条留痕。「没有同意就不该有照片」三道闸：客人端不勾不让提交 → `TicketFacePhotoService.stageForOrder` 无同意 id 即拒（`FACE_PHOTO_CONSENT_REQUIRED`）→ 库上 `chk_tfp_consent_present`（订单暂存位必须有 `consent_id`，绕过服务直接 INSERT 也写不进）。告知文案改动必须同步 bump `TicketFacePhotoService.NOTICE_TEMPLATE_VERSION`（客人端告知块已加提示注释） | ✅ 已落地（V368） |
| C2 后台核销页显示脸照 | 核销时扫到权益即调（原写「核销预检扫到权益」（原两步「预检 + 确认」流程，**已废止·见文首口径**）） `GET /ticketing/issued-entitlements/{id}/face-photo`，以背景图方式显示供核销员比对（不落前端存储、不进操作结果、看完可一键清除） | ✅ 已落地 |
| C3 模板编辑勾选 `require_face_photo` | ✅ **已落地**（PR #274）：开关已进权益模板**新建**表单 + 列表/详情展示。**既有模板**改此开关仍需批 5 主体的模板编辑页 | 🔶 新建路径已通，编辑路径待批 5 主体 |
| C4（可选）核销完成即提前清照 | 现状仍是 `TicketFacePhotoService:80` purge_after = valid_to + N 天，`TicketFacePhotoPurgeJob` 每日硬删。**V353（2026-07-28）已把 N 从硬编码 30 改成后台可配**（落 `notification_windows`，ADM-ANA-011 可改，加了 3650 天上界）。「核销即清」这个更激进的方案仍未做 | ⏸ 可选优化（底层已改善） |

### D. 场次 / 权益配套

| 项 | 内容 | 状态 |
|---|---|---|
| D1 营业时间配置页 | ✅ 后台「场地营业时间」页（ADM-GOV-008，`/admin/governance/venue-business-hours`）按场地×星期几配开业/歇业时刻、保存即生效；配套 `GET/POST /api/v1/platform/venue-business-hours`（读 `governance.space.view`、写 `governance.space.manage`，每次改动写审计）。「本日不营业」= 停用该日配置（复用 `is_draft` 列），配置行保留可随时恢复，不物理删 | ✅ 已落地（2026-07-28） |
| D2（可选）歇业精确关场 job | ⏸ **确认未做，且是有意为之**：`TicketVerificationService:839` 注释直书「惰性开场——核销时按营业时间表判…**不做后台定时开歇场 job**」。属设计取舍，不是欠账 | ⏸ 消项（设计取舍） |
| D3（可选）多时区 | ⏸ **确认未做，升级路径已写在代码里**：`JdbcTicketingRepository:1026-1027` 用 `ZoneId.systemDefault()`，注释「若日后要按各 venue 独立时区，营业表加 zone_id 列在此换算，接口不变」。单城市场景无需求 | ⏸ 消项（等需求） |

### E. 待用户拍板口径

| 项 | 内容 | 状态 |
|---|---|---|
| E1 中央餐饮开关默认值 | ✅ **已裁定（2026-07-28）：默认=关**——订台低消/二次消费/额外酒水归各酒吧商户。裁定时代码现状即符合（当时 `revenue_stream_attribution` 0 行）。**2026-08-01 注**：该表现已种 4 条全局默认（V370/V372/V375：`ticket_service_fee`/`pass_gift_cost`/`all_pass_ticket`/`amusement_ticket`），但 `table_dining` 仍未配、结算路径不据它改钱归属——E1 裁定不受影响。⚠️ 此归属为可重议项 | ✅ 消项（零代码改动） |
| E2 业务 HTML 同步 | 🔶 **三项中两项已同步、只剩一项**：<br>① 票种规则（新人/学生）**已在 HTML**（`business-enum-quick-reference` + `business-system-map` 均命中）<br>② 退款手续费**取消**——三份 HTML grep「手续费」零命中，与「取消」的新口径**一致**（HTML 不提即正确）<br>③ **游乐票人脸照口径未写进 HTML** —— 三份 HTML grep「人脸照」零命中，只有泛化的 PIPL「人脸…敏感信息需单独同意」，**没有「游乐票购票须拍脸照存档」这条业务口径** | ⬜ 只剩 ③。⚠️ **改 HTML 必须先出逐句草案给用户逐条批准** |

### F. 设计性延后（等外部条件，非欠功能）

| 项 | 状态 |
|---|---|
| 真实短信供应商适配器 | ⏸ `notification` 包只有 `SmsSender`（接口）+ `ScaffoldSmsSender`（脚手架）。**等报备模板 ID，外部条件未满足，非欠功能** |
| IoT / 远程表硬件门控 | ⏸ `FacilityEquipmentService:31` 已有 `ALARM_SOURCES = Set.of("manual","iot")`，告警源枚举与结构预留就绪。**等网关/设备** |
| 第三方聚合支付 / 视频监控 / 团购渠道 / 税务开票 | ⏸ 预留接口 + 开关默认关，开通即生效 |
| 平板装机 | ⏸ 等硬件 |

### G. 可选 / 低优

| 项 | 内容 | 状态 |
|---|---|---|
| G1 纯英文 ApiException 逐条中文化 | ❌ 真缺口。**实际 1012 处**（原写 ~1210 偏高）：全后端 `ApiException.xxx("CODE","msg")` 共 **1115** 处，其中不含任何中文字符的纯英文消息 **1012** 处（已中文化 103 处）。前端已有通用中文兜底（不漏英文但丢具体原因）。约 2~3 天，**机械但量大，且每改一条都可能踩到断言消息文本的测试** | ⬜ 低优 |
| G2 BI 遗留 | 🔶 **一半已完成**：**异常检测的处置流转 = ✅ 已完成**（V306 `metric_anomaly_disposition` + `AnalyticsService:973` 处置状态机 open→acknowledged/resolved/false_positive/escalated、终态重复处置抛 409 + `AnalyticsRepository:82` 行锁串行化 + `AnalyticsService:1024` append-only 处置时间线 + 前端 `BusinessAnalyticsPage.disposition.test.tsx`）。<br>**`analytics_dashboard_snapshots` 旧表仍被 `JdbcAnalyticsRepository:155,168` 正常读取，不是死表**；「旧表处置」这个决策从没做过，但**也不是缺功能** | 🔶 处置流转消项；旧表 ⏸ 待决策（非缺口） |
| G3 收官教学 | 🔶 逐页引导已有（`GuidedFlowModal` 已接 **38 页**），但**没有任何「完整使用说明 / 帮助 / 教学」页或路由**（`adminRoutes` 无 ADM-HELP 类页号）。**依赖前置（系统全做好后再做），现在做为时过早** | ⏸ 待前置 |
| G4 NAS 恢复演练 | 运维项，用户已裁定搁置 | ⏸ 搁置 |

### H. 台账之外、别处登记但同样过期的四项（2026-07-28 新并入本表）

> 这四项原本只登记在记忆/其他文档里，本轮一并核实后并入，免得下次又从别处翻出来当新缺口。

| 项 | 状态 |
|---|---|
| **库存调拨 / 借调 + 跨主体结算** | ✅ **后端已完成**（记忆台账写「待立项建模」严重过期）：**V277** 建 4 表（`supply_entities` / `inventory_transfer_orders` / `inventory_transfer_lines` / `inventory_trace_ownership_history`）+ **V278** 只写硬化；完整生命周期端点 `GET/POST /inventory/supply-entities`、`/transfer-orders`、`/ship`、`/receive`、`/return`、`/cancel`（权限点 `inventory.transfer_order.manage`）；**跨主体结算已接**——`InventoryService:1775 recordTransferSettlementOnShip`（2026-07-08 拍板：发货即生成应付、借调「借」默认不计占用费，并入现有应收应付台账）、`:1743 reverseTransferSettlementOnCancel` 取消冲正；开发库有真实数据（supply_entities 2 行 / transfer_orders 14 行 / transfer_lines 14 行）。<br>❌ **唯一缺口 = 前端**：全前端 grep `transfer-orders` 零命中，`ADM-INV-001 库存台账` 页不含调拨——**运营在后台点不到调拨，只能靠 API**。约 1.5~2 天（一个调拨工作台页：单据列表 + 建单 + 发货/收货/退回/取消四动作 + 结算联动展示） |
| **商户端 Flutter** | ❌ **真缺口，确认未开始**：全仓 `find -name pubspec.yaml` 零命中，无任何 Flutter 工程。安卓侧只有 `apps/android-terminal`（Kotlin 原生） |
| **BYOD 二期员工端 App 8 项** | ✅ **已完成**（记忆台账过期）：`docs/android-terminal-breakdown.md` §4.2.1 圈定的 Wave1 五类能力，后端端点 + 安卓 App 客户端全部落地。后端 `StaffController`：`/shifts/mine`、`/attendance/mine`、`/attendance/month-view`、`/payroll/mine`、`/payroll/mine/commissions`、`/leaves/mine`、`GET+POST /profiles/mine`；安卓 `TerminalApiClient.kt` 有对应 11 个方法，`MainActivity.kt` 逐个都有调用点 |
| **真实短信供应商适配器 / IoT 硬件门控** | ⏸ 见 F 节，判定不变（等外部条件） |

### 本轮（2026-07-28）实际动了什么

| 项 | 动作 |
|---|---|
| A4 | 后台行高亮类统一为 `admin-row-selected`，删掉并存的 `admin-row-focus`（1 条 CSS + 3 页 + 1 测试） |
| A5 | 下钻验收写成新页准入条目（矩阵 §1 原则第 7 条 + 新增 §3.6 + §5 不得上线条件） |
| 批 7 | 清掉 3 份文档 12 处废弃名词；`authority-rule-ledger.csv` 5 行按现行 HTML 重抽（正文 + 行号校准） |
| 本表 | 按 `.160:~/backlog-truth-sweep-20260728.md` 全量回写：**7 项消项**（批 6a 渠道费 / 批 7 / 批 8 / 批 9 / A'''1 / E1 / BYOD 二期）、**A6 整条作废**（3 个页号不存在）、**迁移号 V232→V357**、G1 条数 ~1210→1012 校正、库存调拨等 4 项并入 H 节 |
| C3 | 未动——与并发代理的「批 5 三尾项」是同一项（同一文件 `TicketingPoliciesPage.tsx`），归批 5 一并做，避免重复实现 |


### I. Codex 双轨审计核验后遗留（2026-08-03）

> 来源：2026-08-03 对 Codex 只读双轨审计报告的逐条核验（20 条结论 → 13 条属实/部分属实、7 条证伪或降级）。
> 已修 14 项见下方「本轮（2026-08-03）实际动了什么」；本表只登记**核验属实但本轮未做**的部分。判定与逐条证据见 PR #320~#324 正文。

| 项 | 内容 | 状态 |
|---|---|---|
| I1 结算 fail-open 归错桶 | `JdbcSettlementBillRepository` 的 `aggregateFormatRevenue`(:186) 与 `aggregateMerchantStats`(:266) 仍是 `COALESCE(merchant_type,'bar')`：商户档案查不到的核销行会被归进酒吧桶、进可分配池分给各酒吧。需与计提闸判据**三处同时**改 fail-loud（只改其一会让判据分叉、K20 守恒立刻炸），并补 `SettlementFeePerTicketS3Tests` / `SettlementServiceFeeConservationTests` 夹具的真商户行 | ⬜ 用户 2026-08-03 拍板「回头修改，计入待办」 |
| I2 成长值独立账（HTML-05 真缺口） | 全仓「成长值」零实现：成长值即积分本身（还吃了等级倍率与生日倍率）、保级窗口是固定周期清零重攒而非 HTML 要求的滚动统计周期（按项目可配，出厂默认十二个月）、票务积分未按票种规则算、降级前按配置天数（按项目可配，出厂默认三十天）通知未实现。本轮只做了「降级一次落到位」止血（#320） | ⬜ 用户选定本轮范围外 |
| I3 规则账本重建 + HTML 哈希门禁 | `docs/prd-audit/authority-rule-ledger.csv` 844 条仍按旧版 HTML 提取（其中 6 行标 AUTHORITATIVE 却引用旧行号，照读会得出与现行 HTML 相反的口径）；`tools/verify-delivery-gates.mjs` 不比对三份 HTML 的 sha256，文档门永远绿 | ⬜ 用户拍板：门禁等账本重建后再加（现在加会立刻挂红） |
| I4 尾差责任主体只写不读 | V383 已落 `rounding_residual_entity_id`，但结算审阅查询与 `SettlementReviewPage.tsx` 只展示尾差金额、不展示责任主体——业务人员对账时看得到尾差、看不到谁认这笔 | ⬜ |
| I5 父票到期终态缺失 | `forfeitEntitlement` 只更新 `issued_entitlements`，父票 `ticket_instances` 到期后状态仍停在 `active`，父票层面看不出已过期/已作废（既有行为，现已被 #323 的新断言显式固定） | ⬜ 待定口径 |
| I6 V 号门禁未进 CI | `verify:migration-version-order`（#320）只挂在 `verify:delivery-gates` 本地跑；GitHub Actions 默认浅克隆取不到 `origin/main` ref，进 CI 需把 checkout 改 `fetch-depth: 0` | ⬜ |
| I7 docs 残留废止别名 | `member-loyalty-and-stored-value-development-design-v0.1.md`(L13/18/23/56/81) 与 `business-html-source-of-truth-acceptance-spec-v0.1.md`(L22/58) 共 7 处仍写「成长积分」（CONTEXT.md 明列的禁止别名）；`docs/prd-audit/` 下另 15 处属审计历史留痕，**不得改** | ⬜ |

### 本轮（2026-08-03）实际动了什么

| 项 | 动作 |
|---|---|
| 综合服务费 | 计提闸加「提供方商户业态=酒吧」条件、封顶收窄到专属单一酒吧门票（#324；依据 HTML enum:47 / map:743,1495-1499） |
| 并发 | 综合服务费与赠送成本的读-算-写加 `pg_advisory_xact_lock`；**未**加唯一索引（会堵死「冲正后该票可重收一道」的合法路径，且对跨票封顶桶无效） |
| 尾差 | V383 加责任主体快照列；渠道费分摊尾差不再由酒吧份额静默吸收（依据 HTML gov:301 / map:1536，该口径 2026-08-01 已从「归项目公司/平台」翻转） |
| 传媒业态 | V384 业态枚举加 `media`、演出票未核销款归传媒公司、业态解析不出时 fail-loud |
| 退款 | 删掉「未实际兑付先记 success」，一处改三条路径（自助/人工/到期）同时生效；新增确认拨付端点、退款列表端点与后台「待确认拨付」面板（#323；依据 HTML enum:37） |
| 到期票 | 到期一律作废、不再自动退款；UI 与 OpenAPI 去掉「到期未用自动退」（依据 HTML enum:39-40） |
| 报错中文化 | 收口在 `GlobalExceptionHandler` 出口翻译（`FieldLabels` 405 键，覆盖 `Strings` 975 处调用 + 96 处字面量 + 7 份本地助手，安卓端零改动受益）；V385 权限展示名中文化 18 条（#322） |
| 终端闸 | 终端总开关与设备凭证改用服务端可信身份、写请求解析不出身份即拒；连带修 `DeviceCredentialBodyCachingFilter`（原会让签名请求自己被挡）；高危授权审批全局逃生口封死（#321） |
| 迁移治理 | 新增 `verify:migration-version-order` 门禁：本分支新增 V 号必须大于 `origin/main` 最大号（#320） |
| **未采纳** | Codex 报告 STD-02（结算留痕物理删）/ STD-03（配置物理删）/ HTML-06（内部价目录未按项目独立）三条经核验**不属实**，未改：前两条删的是幂等重算的自身派生行与 `is_draft=TRUE` 草案槽（真留痕在 `fund_ledger` / `dividend_statement_lines` 的触发器 + REVOKE 双锁），照改等于给结算重算与配置表单塞四道无用审批闸；HTML-06 的 V379 是署名决策且约束比 HTML 更严，照改反而与 HTML:143「物理项目与运营公司多对多」冲突 |


### J. 代码轮（2026-08-07 审计对话移交）

> 来源：2026-08-07 PRD 真实性审计对话收官时移交的代码轮欠账。该对话为**纯文档轮，一行代码未改**；
> 两份只读现状调查报告已随本次登记入库：`docs/prd-audit/rounds/final/code-round-input-money.md`（钱路 10 项 + 4 条新发现）
> 与 `docs/prd-audit/rounds/final/code-round-input-nonmoney.md`（非钱路 50 条 + 2 条新发现）。
> 逐条判定与证据见 `docs/prd-audit/rounds/r8/findings.csv`（finding_id 见各行「出处」）。

| 项 | 内容 | 状态 |
|---|---|---|
| **J1 分账记账期修正（FINAL-01）** | `backend/src/main/java/com/complex/platform/settlement/SettlementPostingService.java:237` 把分账记账期 `accounting_period` 取为 `dueDate`（付账日 = 过账日 + 30 天）所在月，而非结算期 `period_start` 所在业务账期，记账期系统性后移约一个月（7 月分账落 9 月）；下游污染**分红净利润基数**与**租金挂接**。**过账管道 2026-07-21 起已开启**（`settlement.monthly.posting.enabled=true` 在 `~/.complex/complex.env`），属每月真实发生的涉钱偏差。改法：`accounting_period` 改取结算期 `period_start` 所在月。量级 **S**（零风险单点）。出处：findings `FINAL-01` + `code-round-input-money.md` §新发现 1 | ⬜ 待开发，**代码轮批 1 首位** |
| **J2 尾差铁律裁定（FINAL-08）** | 验收规范 `docs/business-html-source-of-truth-acceptance-spec-v0.1.md` 铁律 `RULE-COMMON-MONEY-01` 写「多方拆分尾差由项目公司/平台承担并单列留痕」「尾差不进入任一参与方」，与 PGM「全局原则 · 金额一律精确到分」条**现文方向相反**——现文禁止「默认转给城市项目公司、顶层平台或其他无关主体」，尾差恰恰要进入该笔业务实际承担结算责任的主体（跨域组合通票即售票收款主体），且负尾差由同一主体退回（旧口径无负尾差退回这一档）。**顺序**：①用户裁定口径 → ②改铁律句 → ③顺查代码是否已有按旧口径实现的尾差记账（V383 `rounding_residual_entity_id` 已落列，需核实写入方向）。**裁定前不得据该铁律做尾差相关验收。** 出处：findings `FINAL-08` | ⬜ **待用户裁定**（先裁后改，不得先改代码） |
| **J3 `face_verify_result` 落库核查（FINAL-26③）** | `docs/compliance-and-personal-information-protection-requirements-v0.1.md` §2.2 规定入场核验落 `face_provider_code` / `face_provider_result_id` / `face_verify_result`（**已满 18 岁／未满 18 岁**两档）/ `face_verify_error_code`。**依据：2026-08-07 用户裁决 B-05／B-01（`DEC-R8-85`，`docs/prd-audit/business-decision-ledger.md` L1771 = B-01、L1806 = B-05）。**`FINAL-26` 第 ③ 点的原定性半句已删去，**改判为「缺条」**——`face_*` 字段本身保留，缺的是人工抽查期间三不保存的约束；`face_verify_result` 值域**收敛为两档：已满 18 岁 / 未满 18 岁**，删除「不确定」档（合规文档 §2.2 本体已改毕）。动作由「删列＋迁移」改为「**保留字段 ＋ 补齐约束 ＋ 补缺条**」：**保留 `face_*` 字段，不得删列、不得迁移、不得做数据清理**；补齐 BSM「年龄核验」条的**三不保存**（人工抽查期间不保存身份与证件信息、不保存个人查验结果、不保存抽查汇总数据）作为约束并补上该缺条，措辞按 B-06「逐字转写、零自创」，现成中文转写见 `docs/prd-audit/business-decision-ledger.md` §18。原始人脸照片是否保留取决于第三方人脸识别 SDK 允不允许，平台不自设开关（B-01）。出处：findings `FINAL-26`（关联 DEC-R8-83、FINAL-12） | ⬜ 待核查（**⚠ `FINAL-26` 在 `findings.csv` 已标 `resolved`，本条不再被任何按未闭合缺口出的统计捞出，须人工盯**） |
| **J4 代码轮批 0~4 分批方案（索引）** | 完整分批见两份输入报告，不在此重复：钱路 `code-round-input-money.md` §分批建议（**批 0** 待裁 5 问 / **批 1** 零风险 S / **批 2** 钱在错动 / **批 3** 单域 M / **批 4** 高危 L）；非钱路 `code-round-input-nonmoney.md` §代码轮建议分批（50 条 + 文档批 13 条）。**批 2 前置项四条并列**：①企微「到期销毁」路径下线（`WecomRetentionJob` + `applyRetention` 真删/真匿名，prod 库 `wrp_global_default`/180 天/anonymize/enabled=true 已在，违反永久保存裁决）②「人工放行」两条链下线（票务 `manual-override` 端点 + 治理 `manual_release_l2`，HTML 明写「系统不提供」而业务人员在后台点得动）③自营标注字段（`merchants` 无自营列；51% 旧实现本就不存在，无需拆除）④税率三审链（`tax_rate_bps` 仅 0~10000 校验、单权限一步可改，复用合同审批链） | ⬜ 索引条目，各批立项时再拆行 |

---

## 1. P1 — 净新待开发（有需求口径、代码未落地）【2026-07-05：本节三项均已完成，存档】

| 项 | 来源 | 待建内容 | 备注 |
|---|---|---|---|
| 外包供应商自助下属门户（业务层） | PRD §4.1、V183 地基 | 机构主体 CRUD、机构管理员自助开通/停用下属、下属数上限校验、账号时效到期自动停用任务、下属继承机构范围 scope 强制、自助门户前端页 | 地基 V183 已落（机构表/管理员角色/权限点）；本层为业务逻辑 |
| 业务短信通知 触发点 + 前端 | `third-party-sms-...placeholders §6`（2026-06-29 新增） | 存酒(存/取/即将到期/已过期)、储值(充值到账/每笔消费扣款,不含积分)、商户费用(房租/水电/物业账单+催缴/续约提醒) 各域触发点；后台两级开关前端 | 发送底座(V177 notification 包+两级开关)已推；储值触发点因储值域并发重建冻结，待解冻接入 |
| 「全链可点击可溯源」P1 下钻 | `admin-page-traceability-clickthrough-audit-v0.1` | contract_id/order_id/merchant_id/审计引用 可点到来源 + 统一 `<TraceLink>` 组件 + navigate 带参 + 反查时间线入口；逐页 Playwright「点击→定位」验收 | 资金/履约/审计链最依赖，先落 |

## 2. P2 — 深化 / 完善项

| 项 | 来源 | 待建内容 |
|---|---|---|
| 全链下钻 P2 | 同上盘点 | ✅ 已完成（2026-07-05，main=43d9bae：项目/空间/处理人/商户门户+补 MerchantZoneBills） |
| 招商租赁补充 | `real-estate-full-chain §7` | 租赁状态看板、招商跟进提醒阈值/分配规则细化 |
| 楼宇运营深化（多数已落地，2026-06-30 核实回写） | `real-estate-full-chain §7` | 已落地：巡检异常→自动整改工单（原子）、工单回访/验收（satisfaction）、巡检排程任务、设备告警事件、抄表异常+父子表差异分摊。**剩余硬件门控**：IoT 自动接入（需网关/设备）、远程表自动读（需远程读表计）——预留事件/结构已就绪，等真实硬件。~~剩余增强项：能耗分摊算法细化、巡检路线排程/点位库细化~~ **已消项（2026-07-03 实读回写）**：能耗分摊三 basis 均已落地（`EnergyService.settleEnergyFee`：by_contract 固定额 / by_area 按面积占比 / by_usage 按用量占比分摊公区能源费，含幂等 + 异常检测 reverse/zero/surge）。**注（2026-08-05）：`by_area`/`by_usage` 所依据的「公区能耗向商户分摊」口径已作废**——公区能耗只归集做成本分析、不向商户分摊，公区公摊统一含在物业费中（ENUM「公区能源费」条、DEC-R7-35）；此处只记录代码现状，**属待改造缺口，不得反向定义业务规则**，收敛口径见 `building-operations-prd-v1.0.md` §5；巡检三排程模式均已落地（`FacilityInspectionService`：template 周期自动排程[`FacilityInspectionScheduleJob` 按 cycleDays 生成] / manual 手动 / route 路线+结构化 `route_points` 点位，route 模式点位强校验非空）。无低成本纯软件增强待做，无净新缺功能。 |
| 酒吧域验收补强 | `bar-operations-development-design §...` | 退菜退酒/反结账 端到端 G/W/T 验收用例；pour_volume 缺采集兜底 + 补采集异常闭环 |

## 3. P3 — 收尾 / 装饰

| 项 | 待建 |
|---|---|
| 全链下钻 P3 | ⬜ 未做（见 0b-A2） |
| 条目 3b 新页 e2e | 实时 Playwright 跑通（矩阵闸已绿、tsc 编译过、属补证） |

## 4. 设计性 gating（**非"未完成"**，预留接口 + 开关默认关，开通即生效）

- 第三方接入：聚合支付、视频监控、团购渠道、税务/电子发票开票、SMS 真实运营商适配器、webhook `mapPayload` 标准化+失败边界+测试锚点、运营商模板 ID 映射表
- 能力开关：年龄核验环节（人脸扫描）、支付渠道、分级退款授权（已建）、酒吧扫码点酒

## 5. 已完成 / 文档过期需回写（消项）

| 项 | 状态 |
|---|---|
| 票实例退款/到期（按票实例、退子集、到期逐张） | ✅ 已落地（2026-06-30，commit 2b044f4），dev-design 已标已落地 |
| 跨对象 audit_events 查询/导出端点 | ✅ 已落地（条目4） |
| 空间类型字典硬校验 + 后台录入 + 下拉 | ✅ 已落地（条目3，ADM-GOV-007） |
| 准入旧表(admission_audits/contract_admission_rules)残留 | ✅ 已清理为附录Z/废止标注 |
| 合同审批流/变更/退租结算/租金账单 | ✅ 已落地（V162）；⚠️ `real-estate-full-chain §7` 仍列「待建」为**过期标注，待回写** |

## 6. 维护说明

- 完成一项后在本表标 ✅ 并消项；新需求按 P1/P2/P3 增行。
- 后端代码层近乎零散落 TODO（grep TODO/FIXME = 0 文件）——待办以本表 + 各域文档为准，不散落代码。
- **标 ✅ 必须同时留证据**（迁移号 / 提交 / 端点 / 页面），否则下一个人无法自证、只能重做一遍。
- **引用页号前先核 `apps/web/src/admin/adminRoutes.ts`**：本表 2026-07-28 查出 3 个根本不存在的页号（A6 原条目），按不存在的页号派活等于凭空造需求。
- **本表抬头必须写清核查基准（main SHA + 日期 + 当前最大迁移号）**；基准与当前 main 差距过大时，先核查再引用，不要直接照着立项。
