# 业务 HTML 源口径验收规格 V0.2

> 生成日期：2026-06-30；v0.2 修订：2026-07-02——按五源审计 SPEC 域回填 §2B 细则级验收映射、清除 HTML 无来源的技术口径与页面名、计数去硬编码。本文件只把三份业务 HTML 的业务事实映射到现有 PRD、开发设计、OpenAPI、数据库、UI 和测试入口；不重写 HTML，不新增业务口径。若本文与三份 HTML 冲突，以三份 HTML 为最终解释权。

## 1. 权威来源与非权威 HTML

| 类型 | 文件 | 用途 |
|---|---|---|
| 业务最终解释权 | `docs/business-system-map-for-operations-v0.1.html` | 面向业务人员的经营系统地图，给出子系统、对象、入口、流程和边界规则。 |
| 业务最终解释权 | `docs/platform-governance-map-for-operations-v0.1.html` | 平台治理底座，给出项目/空间/商户/合同/员工/设备/权限/终端/外包/审计边界。 |
| 业务最终解释权 | `docs/business-enum-quick-reference-v0.1.html` | 业务枚举速查，给出票务、会员、酒吧、游乐、库存、财务、第三方、风控、治理、地产运营状态。 |
| 非业务权威 | `apps/web/index.html`、`apps/web/ticketing/index.html`、`apps/web/bar/index.html`、`apps/web/amusement/index.html` | Vite 多入口应用壳，只能作为前端构建入口，不解释业务范围。 |

HTML 中的可点击控件主要是导航锚点、子系统卡片和“查看说明”链接；业务按钮、表单字段和错误态以 HTML 业务规则映射到下游 UI/验收文档，不从应用入口 HTML 反推。

## 2. 业务域验收映射

| ID | HTML 来源 | 业务对象 | 页面 / 入口 / 可点击项 | 字段 / 状态 / 枚举 | 用户流程 | 边界、错误、复核 | 下游文档与验证入口 |
|---|---|---|---|---|---|---|---|
| HTML-BASE-01 | 业务系统地图「基础管理」+ 治理地图「综合体/空间/商户/合同」 | 综合体项目、楼栋、楼层、区块、铺位、公共区域、商户、合同、员工、设备、权限 | 基础管理卡片；治理拓扑；后台平台/空间/商户/合同页面 | 项目状态、商户状态、合同状态、设备状态、权限状态；四类主体唯一绑定 | 建项目和空间 -> 建商户/主体 -> 合同生效 -> 后续业务准入 | 跨项目/跨商户越权拒绝；合同、空间、业态、有效期越界先卡住再复核 | `platform-governance-prd-v0.1.md`、`foundation-database-design.md`、`p0-operation-permission-matrix-v0.1.md`、`verify:full-admin-matrix` |
| HTML-TKT-01 | 业务系统地图「票务权益」+ 枚举「票务与权益」 | 票种、订单、票实例、权益、防伪二维码、核销记录、退款单 | 票务权益卡片；客人端购票/二维码；工作手机核销；售票型一体机 | 订单状态、票/权益状态、核销记录状态、退款单状态、有效期类型 | 配票价 -> 购票支付 -> 出票 -> 动态码核销 -> 退款/到期处理 -> 财务对账 | 核销最小单位是单个权益；必须在线核销；截图码失效；多人/多次票任一权益已核销则整票不可退 | `ticketing-entitlement-checkin-product-prd-v0.1.md`、`ticketing-pass-entitlement-development-design.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md`、`verify:operation-matrix` |
| HTML-CUST-01 | 业务系统地图「客人端」+ 枚举「会员与积分」 | 客人账号、订单、权益二维码、积分、等级、储值卡、兑换记录 | 客人 App/小程序；订单/权益/积分摘要；礼物目录；自助兑换 | 成长积分、可用积分、兑换发放状态、储值账户 | 登录/绑定手机号 -> 购票/订台 -> 查订单/权益 -> 出示二维码 -> 查询积分/兑换 | 新人按手机号自动判；积分两本账；兑换先扣礼品库存，库存不足拒绝且不扣积分；不支持会员间转赠 | `customer-app-miniapp-product-prd-v0.1.md`、`member-loyalty-and-stored-value-product-prd-v0.1.md`、`full-admin-ui-acceptance-matrix-v0.1.md` |
| HTML-TERM-01 | 业务系统地图「酒吧平板/工作手机」「触摸屏一体机」+ 治理地图「终端开关」 | 平板、工作手机、一体机、员工私人手机 App、商户门户、移动管理端 | 现场端、平板点酒、工作手机扫码、一体机服务/售票入口、终端开关后台 | 终端十端、开关优先级、设备状态、一体机售票能力（售票型/服务型） | 后台配置终端能力 -> 端侧拉取开关 -> 允许对应业务入口 -> 动作留痕 | 拉取失败兜底关闭；非售票型一体机不得售票；酒吧门口/前台一体机只售本酒吧单店票 | `terminal-page-product-prd-v0.1.md`、`operation-traceability-and-terminal-switch-prd-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-BAR-01 | 业务系统地图「酒吧管理」+ 枚举「酒吧经营」 | 酒吧、桌台/卡座/吧台位、订台、账单、点酒订单、出品票据、班次、小费、提成、存酒 | 酒吧管理卡片；订单/出品/账单页；平板点酒；收银交班 | 台位状态、账单状态、订台定金状态、出品票据状态、收银交班状态、支付方式 | 入场/开台 -> 点酒 -> 出品 -> 支付/小费 -> 拆合单/结账 -> 班结对账 | 低消不被票务权益抵扣；高危反结/沽清/免单/调账需审批；出品后退品按是否回库；小费不计低消和提成 | `bar-business-operations-product-prd-v0.1.md`、`bar-operations-development-design.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`verify:full-admin-matrix` |
| HTML-INV-01 | 业务系统地图「供应链库存」+ 枚举「供应链与库存」 | 供应商、供应合同、采购、库房、物品、库存流水、高价值物品、客人存酒、礼品库存 | 供应链库存卡片；库存台账；存酒登记/领取/作废 | 供应合同模式、库房分类、物品大类、库存流水类型、高价值判定 | 入库资质 -> 贴码交接 -> 出库/销售消耗 -> 盘点/报损 -> 存酒/礼品兑换 | 食品/酒类入库绑定资质票据批次；高价值报损/盘点差异/存酒作废须照片+视频+主管复核 | `supply-chain-inventory-product-prd-v0.1.md`、`supply-chain-inventory-development-design-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-AMU-01 | 业务系统地图「游乐管理」+ 枚举「游乐运营」 | 游乐项目、场次、容量、核销、巡检、检修、事故/异常、团体/包场 | 游乐管理卡片；项目/场次/安全/巡检页面 | 项目开放状态、场次状态、容量双阈值、安全严重度、巡检任务状态 | 配项目/场次 -> 开放前巡检 -> 入场核销 -> 容量控制 -> 异常整改/复检 -> 主管恢复 | 安全容量硬上限任何情况阻断并自动停运；当日无合格巡检或未复检阻断项拒绝核销 | `amusement-business-operations-product-prd-v0.1.md`、`amusement-operations-development-design.md`、`amusement-p0-safety-inspection-spec-v0.1.md` |
| HTML-MER-01 | 业务系统地图「商户管理」+ 治理地图「商户侧」 | 酒吧商户、游乐商户、餐饮/零售商户、商户员工、商户侧业务范围 | 商户管理卡片；商户门户；商户侧 App；商户看板 | 商户类型、运营主体状态、合作模式、商户可见范围 | 建商户档案 -> 授权业务范围 -> 商户使用本商户经营/库存/对账/看板 | 商户不得看平台全局、其他商户和跨项目数据；授权范围内全开放，范围外 fail-closed | `platform-governance-prd-v0.1.md`、`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv` |
| HTML-LEASE-01 | 业务系统地图「招商租赁」+ 枚举「地产运营」 | 招商线索、品牌/客户库、意向、合同、合同变更、退租结算、周期账单 | 招商租赁卡片；招商工作台；合同工作台 | 招商线索状态、意向阶段、品牌库类型、合同变更类型、退租结算状态、合同审批链 | 线索 -> 意向 -> 洽谈/报价 -> 签约转商户 -> 合同生效 -> 变更/续租/退租/账单 | 不支持转租，换租户先退租再招商；审批链可配分组并行，任一节点驳回退回起草 | `real-estate-full-chain-domain-prd-v0.1.md`、`leasing-acquisition-prd-v1.0.md`、`building-operations-prd-v1.0.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-FAC-01 | 业务系统地图「物业服务」「楼宇运营」+ 治理地图「楼宇运营」 | 物业工单、设备台账、维保、告警、能耗表计、抄表、楼宇巡检、整改 | 物业服务卡片；楼宇运营卡片；外包人员工单/巡检/进场报备入口 | 工单状态、物业工单类型、设备状态、设备维保类型、巡检任务状态、楼宇巡检类型、能耗异常、外包指派范围 | 报修/投诉/告警/巡检异常 -> 派单 -> 处理 -> 回访/验收 -> 统计；抄表 -> 归集 -> 能源费 | 设备告警和重类巡检异常转工单；检修后主管确认才能恢复；能耗抄表归能耗域，不重复放巡检域 | `building-operations-prd-v1.0.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-3P-01 | 业务系统地图「第三方接入」+ 枚举「第三方接入」 | 餐饮 POS、零售 POS、机器人、视频监控、企微、第三方售票、聚合支付、刷脸、短信、微信/支付宝、税务/电子发票 | 第三方接入卡片；开放接口；集成开关 | 接入状态、回流结果状态、统一原则、开票状态 | 预留接口/开关默认关 -> 身份核验和授权 -> 回流订单/核销/退款/对账/证据 -> 差异处理 | 不自研成熟外部系统；不伪造对账结果；当前无真实第三方接口资料，先建自有系统与开放接口，供应商资料到位后再补数据对应关系、回传格式和对账口径 | `third-party-business-access-product-prd-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`、`integration-capability-switches-v0.1.md` |
| HTML-RISK-01 | 业务系统地图「留痕与异常」+ 治理地图「留痕复核」+ 枚举「留痕与风控」 | 操作事件、证据、风险告警、复核队列、敏感查看、审批/放行记录 | 留痕与异常卡片；操作留痕查询；风险提示；复核队列 | 风险四级、事件状态、主管复核强制项 | 关键动作发生 -> 自动留痕 -> 风险分级 -> 复核/驳回/关闭/归档 | 事件只能作废/冲正不能删除；人工放行、免单、退款、调账、高价值处置、敏感查看强制复核 | `evidence-risk-control-product-prd-v0.1.md`、`evidence-risk-layer-spec-v0.2.md`、`p0-action-event-evidence-matrix-v0.1.md` |
| HTML-FIN-01 | 业务系统地图「财务对账」+ 枚举「财务结算与对账」 | 应收应付、支付/退款、日结、差异、结算、发票、租金/物业费/能源费账单 | 财务对账卡片；AR/AP 台账；对账/日结/结算页面 | 对账口径、对账差异四类、日结状态、差异处理状态、结算状态、开票状态、退款资金流向 | 业务单据确认 -> 生成台账 -> 对账 -> 差异处理 -> 日结锁定 -> 结算/开票 | 日结锁定后不得改原始金额；非原路退款触发风控；税务/电子发票走第三方对接，不自研开票引擎 | `finance-settlement-product-prd-v0.1.md`、`finance-ar-ap-ledger-development-design-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-DASH-01 | 业务系统地图「经营看板」 | 收入、客流、核销、异常、商户表现、对账、铺位出租率、设备/能耗指标 | 经营看板卡片；集团/经营总览；商户看板 | 指标分类、角色可见范围、下钻维度 | 汇聚业务事实 -> 按权限展示 -> 下钻追问 | 看板数据必须来自真实业务事实；指标上游未接入时必须如实标注来源未接入，不得冒充真实聚合 | `business-dashboard-metrics-product-prd-v0.1.md`、`data-analysis-layer-spec-v0.2.md`、`runtime-acceptance-evidence-v0.1.md` |
| HTML-GOV-01 | 治理地图「权限和职级」「外包人员与审计人员」「终端开关」+ 枚举「平台治理」 | 角色、职级、员工、外包人员、审计人员、权限模板、临时授权、终端开关 | 权限和职级卡片；外包人员操作入口；审计记录查询；终端开关后台 | M1-M10 三十档、最终权限计算、终端开关优先级、员工状态 | 配角色/模板 -> 员工单独授权/拒绝 -> 临时授权 -> 终端开关下发 -> 审计/外包按范围操作 | 显式拒绝优先；外包只看被指派范围；审计人员只读和敏感查看留痕；开关拉取失败兜底关闭 | `platform-governance-product-spec-v0.2.md`、`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv` |

## 2B. 细则级验收映射（2026-07-02 v0.2 增补，回填五源审计 SPEC 域缺口）

> 域级映射（§2）回答"哪个域归哪些文档"；本节把三份 HTML 中可直接验收的边界细则逐条落到断言，验收/审计按行回链。规则原文以 HTML 行号为准，本表只做摘要。

| 细则 ID | HTML 来源 | 规则要点（摘要） | 可验收断言 | 归属域 | 验证入口 |
|---|---|---|---|---|---|
| RULE-TRACE-01 | 治理:270、778 | 平台内所有数据、单据、对象、人员与角色可点击、可追溯、可正查反查，不允许孤立数据 | 核心列表页字段可点击进入来源对象；目标页支持定位高亮；抽查无孤岛单据 | 全部域 | TraceLink 组件铺设（3.9）、`verify:full-admin-matrix` |
| RULE-TKT-REFUND-01 | 业务:731 | 退款按金额与规则分三档：规则内系统自动退；员工固定额度内自助退；超额度转人工复核，复核人不得为发起人；各综合体独立配置（票务、酒吧、财务统一适用） | 三档路由可配可验；自审自批被拒；发起人与审批人分别留痕 | TKT/BAR/FIN | 退款分级授权 Stage B（1e5ea30）、`p1-ticket-refund-line-ledger-api-contract-v0.1.md` |
| RULE-CUST-CONSENT-01 | 业务:795 | 采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，留同意版本、时间和范围 | 同意记录含版本/时间/范围三要素；未同意不采集 | CUST | `POST /customer/consents`、customer PRD |
| RULE-TKT-QR-01 | 业务:796 | 二维码原始值不存在手机本地、只在线核验；截图/复制码无效 | 客人端 DOM/本地存储无凭证原始值；核销仅在线；过期码被拒 | TKT/CUST | 1.7（TicketQrView 测试断言）、动态码 10/15/30/60s 四档刷新（默认 15s，2026-07-18 拍板） |
| RULE-BAR-AGE-01 | 业务:926 | 门口入场统一刷脸核验，全场 18+、未成年拒入；刷脸开关关闭期间制度兜底；售酒环节继承入场核验结果，不逐单验年龄、不做醉酒拒售 | 入场闸按开关核验；售酒动作读入场核验结果不再验龄；开关关闭走制度兜底口径 | BAR | 年龄合规与人脸入场闸（V127 簇）、`amusement-p0-safety-inspection-spec` 同型开关范式 |
| RULE-BAR-POUR-01 | 业务:920 | 按杯销售实际出酒量强制采集；缺采集拒单或按标准量兜底并留异常 | 按杯单无出酒量→拒单或标准量兜底+异常留痕 | BAR/INV | 酒吧出品链路 PRD、留痕事件矩阵 |
| RULE-BAR-INVITE-01 | 业务:776、894 | 订卡邀请函二维码含酒吧、卡座、可进人数；可转发；朋友确认后占名额；复制/篡改即无法入场；已确认+已入场不得超可进人数 | 邀请函确认占名额；篡改签名校验失败拒入；人数上限硬校验 | CUST/BAR | 邀请函 HMAC 签名（票 HMAC 同族）、BarInvitationPage |
| RULE-BASE-CERT-01 | 治理:471、478 | 证照、资质、保险、合同关键日期到期预警与到期阻断；酒类/食品资质必填；合同先线下纸质签署、系统上传扫描件归档 | 到期项预警可见；到期后相关准入被阻断；合同档案含扫描件 | BASE/LEASE | 合同域 V162、准入闸（AdmissionGuard） |
| RULE-FAC-SLA-01 | 业务:1107、1109 | SLA 按工单类型×优先级矩阵配置，超时自动升级；验收闭环按工单类型；满意度由提单方评分 | SLA 矩阵可配；超时升级事件留痕；验收/满意度字段按类型呈现 | FAC | `building-operations-prd-v1.0.md` §工单 |
| RULE-FIN-SOURCE-01 | 业务:1340 | 平台侧金额必须系统自动取数、不许手填；第三方侧接入前可人工录入但须标记并强制复核 | 平台单据金额只读自动取数；人工录入带标记且必经复核 | FIN | `finance-ar-ap-ledger-development-design`、对账差异复核链 |
| RULE-SMS-01 | 业务:1203、1236-1238 | 短信除验证码外覆盖存酒、储值动账、商户费用账单/催缴、合同到期续约提醒；支持整类或单条开关 | 三域事件触发短信；总/域/事件三级开关生效 | 3P | 短信通知三级开关（三级开关已落地）、`third-party-sms-notification-…placeholders` |
| RULE-WECOM-01 | 治理:781 | 企微后台由管理员一次性完成授权与安全凭据配置；系统自动账号对应、同步、失败告警、基础规则质检、会话留存策略 | 授权配置页可完成凭据配置；同步失败产生告警；质检规则可配置 | 3P/GOV | 企微合规域（WecomConversationService）、留存策略可配 |
| RULE-AMU-LIMIT-01 | 业务:1029、1032 | 游乐使用限制（身高、年龄、健康、安全告知、人数）现场核验留痕；团体/包场须主管审批；重开/异常关闭/包场触碰越界空间先卡住逐级复核 | 限制项核验留痕；团体/包场无审批被拒；越界空间动作进复核 | AMU | `amusement-business-operations-product-prd` §使用限制 |
| RULE-GOV-AUTH-01 | 治理:556-576 | 四类高危权限授出前须审批才生效且不可配置关闭；临时授权可设有效期或单次使用，授予/收回/到期全留痕；外包人员只在被指派范围操作；审计人员内稽全量只读/外审受限范围，敏感查看留痕 | 高危授权未审批不生效且申请人不能自审；临时授权到期/用后失效；外包越范围被拒；审计敏感查看留痕 | GOV | 平台治理 PRD §12、集团治理 PRD §3、外包/审计角色验收 |
| RULE-TERM-REJECT-01 | 治理:353+终端开关节 | 终端开关关闭后后端拒绝对应业务动作并留痕；端侧拉取失败兜底关闭 | 关端/关模块→接口 403 并写 terminal.access_rejected；前端拉取失败隐藏入口留自救组 | TERM | 3.19 二期（ea7ce89）：拦截器+V198+菜单 gating+活证据 |
| RULE-TKT-TYPE-01 | 业务:679、712 | 平日/周末/节假日/学生/多人/一人多次票可配；学生票人工审核证件+学生证；按人群资格判定 | 票种矩阵可配置；资格判定路径（如学生审核）可验收 | TKT | 3.10 票务策略配置化（972a50a） |
| RULE-MEM-01 | 业务:743-744 | 积分两本账：成长积分只增不减用于等级，可用积分消费抵扣；等级按累计成长积分门槛自动升级 | 两本账独立记账；等级评估按成长积分门槛 | CUST | `member-loyalty-…prd`、LoyaltyService 等级/过期批处理 |
| RULE-COMMON-ZH-01 | 治理:286 | 所有终端的用户可见文字全部为中文，只有机器识别的编码、ID、单据号等可保留原样 | 抽查菜单、字段、状态、枚举、按钮、提示、报错、导出、短信、通知和日期时间；出现非技术标识英文即失败 | 全部域 | 产品总纲 §14；各终端 UI 验收 |
| RULE-COMMON-MONEY-01 | 治理:301 | 所有金额计算、存储和显示精确到分；多方拆分尾差由项目公司/平台承担并单列留痕 | 金额四舍五入到两位小数；拆分总和与原金额一致；尾差不进入任一参与方且有独立记录 | 全部金额域 | 产品总纲 §14；财务 PRD |
| RULE-COMMON-CONCURRENCY-01 | 治理:302 | 并发冲突以先处理成功为准；重复提交只认一次 | 并发测试仅一个动作成功；重试不重复扣款、入账、核销或消耗权益 | 全部交易域 | 产品总纲 §14；operation 级幂等验收 |
| RULE-COMMON-ARCHIVE-01 | 业务:562-565 | 存档不需审批；删除须先存档、填写理由、由另一人审批且不得自审；删除只隐藏不物理抹除；存在业务占用时阻断 | 存档可直接执行并可恢复；删除缺理由、自审或有占用均被拒；删除后历史可查、可审计、可恢复 | 全部对象域 | 产品总纲 §14；业务决策日志历史校正 |
| RULE-GOV-ROLE-CHANGE-01 | 治理:603-605 | 职位/层级决定角色和权限模板并自动适配；高危授权按主管层级审批 | 岗位、职级、组织、商户或责任范围变化后立即重算；旧范围派生权限失效；授权申请人不能审批自己的申请 | GOV | 平台治理 PRD §6.10/§7.8/§12；集团治理 PRD §3 |

## 3. 验收使用规则

1. PRD、开发设计、OpenAPI、数据库、UI、测试若与三份 HTML 冲突，先标为待修正文档或实现，不反向解释 HTML。
2. 每个新业务修复必须能回链到上表某个 HTML ID；若找不到 HTML ID，先按业务不明确项提问，不直接实现。
3. OpenAPI operation 数以 `npm run verify:operation-matrix` / `npm run verify:openapi-closure` 实时输出为准；本文不固化具体数字（历史快照易过期）。
4. 后续后端、前端、UI/UE 阶段按本文件的 HTML ID 复核，不把 `apps/web/**/*.html` 当业务来源。

## 4. 验证证据（2026-07-02 快照；权威以 verify:* 实时输出为准）

| 命令 / 取证 | 结果 |
|---|---|
| `npm run verify:operation-matrix` | 通过；operation 数以实时输出为准（本快照日 540）。 |
| `npm run verify:openapi-closure` | 通过；与 operation-matrix 同步。 |
| `npm run verify:full-admin-matrix` | 通过；页面路由、权限、UI 与测试覆盖齐。 |
| `backend/src/main/resources/db/migration` | 最新 `V198__terminal_web_module_switches.sql`。 |
| 后端全量测试 | 本快照日 1687 tests 全绿（`mvn test`）。 |
