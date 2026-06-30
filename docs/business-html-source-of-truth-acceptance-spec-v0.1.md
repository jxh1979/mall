# 业务 HTML 源口径验收规格 V0.1

> 生成日期：2026-06-30。本文件只把三份业务 HTML 的业务事实映射到现有 PRD、开发设计、OpenAPI、数据库、UI 和测试入口；不重写 HTML，不新增业务口径。若本文与三份 HTML 冲突，以三份 HTML 为最终解释权。

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
| HTML-CUST-01 | 业务系统地图「客人端」+ 枚举「会员与积分」 | 客人账号、订单、权益二维码、积分、等级、储值卡、付费会员、兑换记录 | 客人 App/小程序；订单/权益/积分摘要；礼物目录；自助兑换 | 成长积分、可用积分、兑换发放状态、储值账户、付费会员订阅状态 | 登录/绑定手机号 -> 购票/订台 -> 查订单/权益 -> 出示二维码 -> 查询积分/兑换 | 新人按手机号自动判；积分两本账；兑换先扣礼品库存，库存不足拒绝且不扣积分；不支持会员间转赠 | `customer-app-miniapp-product-prd-v0.1.md`、`member-loyalty-and-stored-value-product-prd-v0.1.md`、`full-admin-ui-acceptance-matrix-v0.1.md` |
| HTML-TERM-01 | 业务系统地图「酒吧平板/工作手机」「触摸屏一体机」+ 治理地图「终端开关」 | 平板、工作手机、一体机、员工私人手机 App、商户门户、移动管理端 | 现场端、平板点酒、工作手机扫码、一体机服务/售票入口、终端开关后台 | 终端十端、开关优先级、设备状态、`kiosk_mode` | 后台配置终端能力 -> 端侧拉取开关 -> 允许对应业务入口 -> 动作留痕 | 拉取失败兜底关闭；非售票型一体机不得售票；酒吧门口/前台一体机只售本酒吧单店票 | `terminal-page-product-prd-v0.1.md`、`operation-traceability-and-terminal-switch-prd-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-BAR-01 | 业务系统地图「酒吧管理」+ 枚举「酒吧经营」 | 酒吧、桌台/卡座/吧台位、订台、账单、点酒订单、出品票据、班次、小费、提成、存酒 | 酒吧管理卡片；订单/出品/账单页；平板点酒；收银交班 | 台位状态、账单状态、订台定金状态、出品票据状态、收银交班状态 | 入场/开台 -> 点酒 -> 出品 -> 支付/小费 -> 拆合单/结账 -> 班结对账 | 低消不被票务权益抵扣；高危反结/沽清/免单/调账需审批；出品后退品按是否回库；小费不计低消和提成 | `bar-business-operations-product-prd-v0.1.md`、`bar-operations-development-design.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`verify:full-admin-matrix` |
| HTML-INV-01 | 业务系统地图「供应链库存」+ 枚举「供应链与库存」 | 供应商、供应合同、采购、库房、物品、库存流水、高价值物品、客人存酒、礼品库存 | 供应链库存卡片；库存台账；存酒登记/领取/作废 | 供应合同模式、库房分类、物品大类、库存流水类型、高价值判定 | 入库资质 -> 贴码交接 -> 出库/销售消耗 -> 盘点/报损 -> 存酒/礼品兑换 | 食品/酒类入库绑定资质票据批次；高价值报损/盘点差异/存酒作废须照片+视频+主管复核 | `supply-chain-inventory-product-prd-v0.1.md`、`supply-chain-inventory-development-design-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-AMU-01 | 业务系统地图「游乐管理」+ 枚举「游乐运营」 | 游乐项目、场次、容量、核销、巡检、检修、事故/异常、团体/包场 | 游乐管理卡片；项目/场次/安全/巡检页面 | 项目开放状态、场次状态、容量双阈值、安全严重度、巡检任务状态 | 配项目/场次 -> 开放前巡检 -> 入场核销 -> 容量控制 -> 异常整改/复检 -> 主管恢复 | 安全容量硬上限任何情况阻断并自动停运；当日无合格巡检或未复检阻断项拒绝核销 | `amusement-business-operations-product-prd-v0.1.md`、`amusement-operations-development-design.md`、`amusement-p0-safety-inspection-spec-v0.1.md` |
| HTML-MER-01 | 业务系统地图「商户管理」+ 治理地图「商户侧」 | 酒吧商户、游乐商户、餐饮/零售商户、商户员工、商户侧业务范围 | 商户管理卡片；商户门户；商户侧 App；商户看板 | 商户类型、运营主体状态、合作模式、商户可见范围 | 建商户档案 -> 授权业务范围 -> 商户使用本商户经营/库存/对账/看板 | 商户不得看平台全局、其他商户和跨项目数据；授权范围内全开放，范围外 fail-closed | `platform-governance-prd-v0.1.md`、`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv` |
| HTML-LEASE-01 | 业务系统地图「招商租赁」+ 枚举「地产运营」 | 招商线索、品牌/客户库、意向、合同、合同变更、退租结算、周期账单 | 招商租赁卡片；招商工作台；合同工作台 | 招商线索状态、意向阶段、品牌库类型、合同变更类型、退租结算状态、合同审批链 | 线索 -> 意向 -> 洽谈/报价 -> 签约转商户 -> 合同生效 -> 变更/续租/退租/账单 | 不支持转租，换租户先退租再招商；审批链可配分组并行，任一节点驳回退回起草 | `real-estate-full-chain-domain-prd-v0.1.md`、`leasing-acquisition-prd-v1.0.md`、`building-operations-prd-v1.0.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-FAC-01 | 业务系统地图「物业服务」「楼宇运营」+ 治理地图「楼宇运营」 | 物业工单、设备台账、维保、告警、能耗表计、抄表、楼宇巡检、整改 | 物业服务卡片；楼宇运营卡片；外包工单/巡检/进场报备 | 工单状态、设备状态、巡检任务状态、能耗异常、外包指派范围 | 报修/投诉/告警/巡检异常 -> 派单 -> 处理 -> 回访/验收 -> 统计；抄表 -> 归集 -> 能源费 | 设备告警和重类巡检异常转工单；检修后主管确认才能恢复；能耗抄表归能耗域，不重复放巡检域 | `building-operations-prd-v1.0.md`、`full-admin-page-permission-field-matrix-v0.1.csv`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-3P-01 | 业务系统地图「第三方接入」+ 枚举「第三方接入」 | 餐饮 POS、零售 POS、机器人、视频监控、企微、第三方售票、聚合支付、刷脸、短信、微信/支付宝、税务/电子发票 | 第三方接入卡片；开放接口；集成开关；供应商字段映射 | 接入状态、回流结果状态、统一原则、开票状态 | 预留接口/开关默认关 -> 身份核验和授权 -> 回流订单/核销/退款/对账/证据 -> 差异处理 | 不自研成熟外部系统；不伪造对账结果；真实资料未到位时 mock/adapter 只能支撑验收，不作为生产证据 | `third-party-business-access-product-prd-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`、`integration-capability-switches-v0.1.md` |
| HTML-RISK-01 | 业务系统地图「留痕与异常」+ 治理地图「留痕复核」+ 枚举「留痕与风控」 | 操作事件、证据、风险告警、复核队列、敏感查看、审批/放行记录 | 留痕与异常卡片；审计事件页；风险告警页；复核队列 | 风险四级、事件状态、主管复核强制项 | 关键动作发生 -> 自动留痕 -> 风险分级 -> 复核/驳回/关闭/归档 | 事件只能作废/冲正不能删除；人工放行、免单、退款、调账、高价值处置、敏感查看强制复核 | `evidence-risk-control-product-prd-v0.1.md`、`evidence-risk-layer-spec-v0.2.md`、`p0-action-event-evidence-matrix-v0.1.md` |
| HTML-FIN-01 | 业务系统地图「财务对账」+ 枚举「财务结算与对账」 | 应收应付、支付/退款、日结、差异、结算、发票、租金/物业费/能源费账单 | 财务对账卡片；AR/AP 台账；对账/日结/结算页面 | 对账口径、对账差异四类、日结状态、差异处理状态、结算状态、开票状态 | 业务单据确认 -> 生成台账 -> 对账 -> 差异处理 -> 日结锁定 -> 结算/开票 | 日结锁定后不得改原始金额；非原路退款触发风控；税务/电子发票走第三方对接，不自研开票引擎 | `finance-settlement-product-prd-v0.1.md`、`finance-ar-ap-ledger-development-design-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| HTML-DASH-01 | 业务系统地图「经营看板」 | 收入、客流、核销、异常、商户表现、对账、铺位出租率、设备/能耗指标 | 经营看板卡片；集团/经营总览；商户看板 | 指标分类、角色可见范围、下钻维度、快照状态 | 汇聚业务事实 -> 生成指标快照 -> 按权限展示 -> 下钻追问 | 看板不能用手工种子冒充真实聚合；指标缺上游时必须标明来源缺失或 mock 边界 | `business-dashboard-metrics-product-prd-v0.1.md`、`data-analysis-layer-spec-v0.2.md`、`runtime-acceptance-evidence-v0.1.md` |
| HTML-GOV-01 | 治理地图「权限和职级」「外包人员与审计人员」「终端开关」+ 枚举「平台治理」 | 角色、职级、员工、外包人员、审计人员、权限模板、临时授权、终端开关 | 权限和职级卡片；外包工作台；审计事件页；终端开关页 | M1-M10 三十档、最终权限计算、终端开关优先级 | 配角色/模板 -> 员工单独授权/拒绝 -> 临时授权 -> 终端开关下发 -> 审计/外包按范围操作 | 显式拒绝优先；外包只看被指派范围；审计人员只读和敏感查看留痕；开关拉取失败兜底关闭 | `platform-governance-product-spec-v0.2.md`、`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`full-admin-page-permission-field-matrix-v0.1.csv` |

## 3. 验收使用规则

1. PRD、开发设计、OpenAPI、数据库、UI、测试若与三份 HTML 冲突，先标为待修正文档或实现，不反向解释 HTML。
2. 每个新业务修复必须能回链到上表某个 HTML ID；若找不到 HTML ID，先按业务不明确项提问，不直接实现。
3. OpenAPI operation 数以 `npm run verify:operation-matrix` / `npm run verify:openapi-closure` 输出为准；本轮核验为 516 operations。
4. 后续后端、前端、UI/UE 阶段按本文件的 HTML ID 复核，不把 `apps/web/**/*.html` 当业务来源。

## 4. 本轮验证证据

| 命令 / 取证 | 结果 |
|---|---|
| `npm run verify:operation-matrix` | 通过；516 operations checked。 |
| `npm run verify:openapi-closure` | 通过；516 operations checked。 |
| `npm run verify:full-admin-matrix` | 通过；43 个页面均有路由、权限、UI 与测试覆盖。 |
| `backend/src/main/resources/db/migration` | 179 个迁移，最新 `V182__merchant_fee_notification_recipient.sql`。 |
| `backend/target/surefire-reports/*.txt` | 现有报告合计 1606 tests / 129 files；本轮未重新声明完整后端测试。 |
| `npx playwright test --list` | 默认配置列出 31 tests in 5 files。 |
