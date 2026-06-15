# 需求覆盖追溯矩阵 V0.1

## 文档定位与依据

> **本文档为什么新增（定位）：** 两轮审计发现现有索引只按"经营动作"粗粒度映射且结论一律"已覆盖"（deliver-2），缺少把 `original-requirements-capture-v0.1.md` 每一条原始口径**逐条**对到承接文档与章节的稳定追溯锚点。本文为每条原始口径编 **REQ-序号**，给出"承接文档+章节 / 覆盖状态 / 备注"，特别覆盖容易遗漏的细则，供外部机器智能体快速核验"哪条需求落在哪份文档的哪一节、是否遗漏"。
>
> **依据：** finding deliver-2、deliver-1（外部智能体评估）；REQ 编号与 `original-requirements-capture-v0.1.md` 各节一一对应；口径冲突一律以 `unified-standards-baseline-v0.1.md`（统一口径基准 D1–D13）与 `business-decision-log-v0.1.md` §32（2026-06-07 A1/A5/A6/A9 拍板）为准。

## 使用说明

- **需求编号 REQ-NN**：稳定锚点，对应 original-requirements-capture 的条目；同一节多条按 REQ-NN-a/b 细分。
- **覆盖状态**：已覆盖 / 部分覆盖 / 有待对齐 / 缺。"有待对齐"=已有文档但跨文档口径冲突，对齐口径见备注与基准。
- **承接文档**省略 `docs/` 前缀与 `-v0.1.md` 后缀以简化阅读。
- 本矩阵为 V0.1 首版，重点保证"易漏细则"全部有锚点；后续可随 PRD 迭代补全更细的页面/字段/状态列。

## 一、门票与购票（original §4）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-01 | **员工不能用平板代客购票**，门票必须客人自己买 | ticketing-entitlement-checkin-product-prd §2/§10；ticketing-pass-entitlement-development-design §2.1/§7.2；business-decision-log §1/§11；operation-traceability PRD 2.3「票务购买」 | 已覆盖 | 8+ 文档口径一致，与"平板可代客点酒"明确区分（ticket 覆盖小结） |
| REQ-02 | 官方 App 和小程序同步建设、共享口径 | business-decision-log §1；web-mobile-app-terminal-extension PRD；terminal-page PRD | 已覆盖 | — |
| REQ-03 | 第三方购票首批仅美团/大众点评/抖音团购回流 | business-decision-log §2；ticketing PRD §5；**system-breakdown §3.4（2026-06-07 已标注携程/旅行社为 P1+ 预留）** | 已覆盖 | ticket-6 已就地标注，消除携程超范围漂移 |
| REQ-04 | 门票多策略（平日/周末/节假日/多人/学生/新人/渠道优惠） | ticketing PRD §5 票务策略表 | 已覆盖 | 各策略具体落地见 REQ-05 |
| REQ-05 | **新人/学生/多人由后台设规则，符合自动判断、不符不可用** | unified-standards-baseline **D9**；business-decision-log **§32 A5**；ticketing PRD §5/§12 | 已裁决并覆盖 | 原"自动/人工/P1"三处冲突（ticket-1/2）已收口：新人=手机号自动判定+短信验证；学生=人工审核+学信网+凭证留痕表；多人=同场次集中核销+num_people 字段。执行口径以 D9/A5 为准。<!-- 修订说明（2026-06-08 三轮收口）：原"§32 A9"/"以 D9/A9 为准"中的 A9 指向已被占给"合规专章"，门票资格 P0=A5（见 decision-log §32 编号更正），两处 A9→A5。依据 finding residual-scan-5、D9/A5 --> |
| REQ-06 | 门票二维码防伪、防篡改、事后核对 | ticketing PRD 防伪票码；terminal-display-layer-spec-v0.2；ticketing dev-design §6.2.3；ticketing-database-implementation-notes；迁移 `002_ticketing_entitlement_settlement.sql` | 已覆盖 | `ticket_credentials` 已补入开发设计和迁移实体，含 `credential_hash`、`anti_fake_code_hash`、签名版本、刷新秒数、有效期、状态和索引；原 fielddb-8 结论已过时。 |
| REQ-07 | 二维码按票种有效期生效（日/周/月/年票） | ticketing PRD §5；business-decision-log §6 | 已覆盖 | — |
| REQ-08 | **二维码 30 秒或 60 秒刷新、截图无效** | business-decision-log §6（"30 秒或 60 秒刷新，截图无效"）；ticketing dev-design §6.2.3 | 已覆盖 | 易漏细则，全目录固定值一致（ticket 覆盖小结） |
| REQ-09 | 门票二维码必须在线核验、P0 不做离线核验 | business-decision-log §6；ticketing dev-design 核销校验 | 已覆盖 | — |

## 二、通票、权益与会员积分（original §5）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-10 | 通票=酒吧/游乐/酒吧+游乐/含餐饮零售折扣组合 | ticketing PRD §6；business-decision-log §4；business-operations-layer-spec-v0.2 | 已覆盖 | 五处口径一致（pass 覆盖小结） |
| REQ-11 | 餐饮/零售为第三方，通票该部分主要是优惠/折扣 | business-decision-log §4；ticketing PRD §6.1；third-party PRD §3/§9 | 已覆盖 | 已显式区分折扣型权益（discount，仅记录优惠资格/折扣口径）与券核销型权益（voucher，P1 接券核销级回流），餐饮/零售真实 POS 仍归第三方。 |
| REQ-12 | 官方 App/小程序会员积分系统、规则后台定义 | ticketing PRD §7.5；business-decision-log §3；foundation-database-design §6B.5；business-operations 五件套 | 已覆盖 | 会员积分五件套与底层表已补：`points_accounts`、`points_ledger`、`points_rules`、`point_gifts`、`point_redemptions`；规则有效期/过期方式与字段字典已按 D7 对齐。 |
| REQ-13 | 积分可与消费、会员等级、兑换礼物相关 | ticketing PRD §7.5；foundation-database-design §6B.5；data-risk-ai §7.1 | 已覆盖 | 跨业态消费入账通过 `points.*` 标准事件与积分明细承载；会员等级在 `customers`/`points_accounts.member_level` 落点。 |
| REQ-14 | **积分兑换礼物需扣减库存** | business-decision-log §3；supply-chain PRD；foundation-database-design §6B.5.5；supply-chain-inventory-development-design §6.13 | 已覆盖 | 兑换成功扣积分、扣 `point_gifts.stock_quantity`，并写 `stock_movements`（movement_type=redeem_consume）进入供应链与成本/对账口径。 |

## 三、酒吧经营（original §6）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-15 | 酒吧可独立成酒吧 SaaS | bar-business-operations PRD；operation-traceability PRD §5（终端开关 SaaS 化） | 已覆盖 | — |
| REQ-16 | 酒吧合同/合作形式（租赁/联营/自营/合作）与收益模式 | platform-governance PRD 合同；business-decision-log §6 | 已覆盖 | 合同摘要由平台治理管理 |
| REQ-17 | 桌台/卡座/吧台位/最低消费/点酒/支付/赠饮/账单/异常/存酒 | bar PRD §5-§9；bar-operations-development-design | 已覆盖 | — |
| REQ-18 | **最低消费按时间/卡座/位置配置，原则上不按人数** | business-decision-log §16；bar dev-design §5.2.2；bar PRD §11 | 已覆盖 | P0 明确不按人数计算，`guest_count` 仅记录；按人数例外作为后置扩展。 |
| REQ-19 | 平板只做点酒下单，必须绑定酒吧+卡座、低消未满不能提交 | bar PRD §7.4；android-device-terminal dev-design；business-decision-log §11 | 已覆盖 | — |
| REQ-20 | 下单生成二维码扫码支付，或平板扫客付款码 | bar PRD 支付处理；bar dev-design §6.10 | 已覆盖 | — |
| REQ-21 | **吧台买酒优先线上支付，但必须支持现金和 POS（中国法律）** | business-decision-log §5/§16；bar dev-design §6.10/§13/§14；operation-traceability PRD §4.1 `payment.cash_received`/`payment.pos_received` | 已覆盖 | 酒吧开发设计已将现金/POS 纳入 P0，记录收款人、POS 流水号，并进入账单、日结、对账和事件中心。 |
| REQ-22 | **按杯销售按标准出酒量/容量扣减，超标记损耗，损耗严重核查** | business-decision-log §16；bar dev-design §6.6/§6.16 | 已覆盖 | （2026-06-07 收口，broken-ref-6）bar dev-design §6.6 已新增 standard_pour_volume/pour_volume_unit/loss_threshold、§6.16 已新增 actual_pour_volume/loss_volume（bar-2），原"缺字段"结论已过时 |
| REQ-23 | 门票赠饮/权益领取与酒吧库存消耗打通 | bar PRD §2/§9；bar dev-design §7.8/§9.4；ticketing dev-design §10.4；p0-end-to-end-acceptance-and-edge-cases §3/§4 | 已覆盖 | P0 确定为酒水权益核销后生成 0 元兑换点单，并在出品时触发吧台库存扣减；赠酒成本由平台承担。 |
| REQ-23b | 酒吧入场/售酒年龄核验强制人脸或等效设备；供应商未定时只能 mock，生产不得正式通过 | compliance-and-personal-information-protection-requirements §2；bar PRD §7.1/§9；bar-operations-development-design §10.1/§15；android-device-terminal-development-design §4/§14.5/§16.1；third-party-supplier-integration-kit-v0.1；p0-end-to-end-acceptance-and-edge-cases §3 | 已覆盖 | 属后续总控合规收口补充项。P0 先保留供应商适配器、mock、字段映射占位和生产阻断；正式上线必须接入第三方人脸/年龄核验接口或等效设备。 |

## 四、订台（original §7）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-24 | 订台入口=App/小程序/一体机/销售工作台 | business-decision-log §5；bar PRD；terminal PRD | 已覆盖 | — |
| REQ-25 | **订台需支付定金** | business-decision-log §5；bar dev-design §6.3/§13.1 | 已裁决并覆盖（P0） | （2026-06-07 收口，newdocs-2）bar-operations §6.3/§13.1 已裁决订台+定金为 **P0**（bar_reservations 与订金规则 P0 落地、补订金支付/到店抵扣流转），前后端口径一致；定金金额/比例/退没收规则以 bar dev-design §13.1 为权威（kiosk-1/2） |
| REQ-26 | 订台记录来源/销售或设备来源/定金/到店/取消/退款 | bar dev-design §6.3 bar_reservations；operation-traceability PRD 2.3「订台和到店」；customer-app §4.3 | 已覆盖 | `bar_reservations` 已补 `order_source`、`created_terminal`、`sales_staff_id`、`device_id`、`deposit_rule`、`deposit_status` 等来源与定金字段。 |
| REQ-26b | 销售人员售卖卡座按消费额 15% 提成，非销售人员按 10%；不含通票 | unified-standards-baseline D13；bar PRD §4/§9/§11；bar dev-design §6.9A/§15；finance-settlement PRD §3/§10；p0-end-to-end-acceptance §3 | 已覆盖 | 新增 `sales_person` 岗位；P0 生成卡座提成可复核台账或应付/内部薪酬依据，复杂多人分摊/阶梯提成/薪资发放后置。 |

## 五、供应链、库房与存酒（original §8）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-27 | 供应链含供应商/合同/供应合同/采购周期/库房管理 | supply-chain-inventory PRD §3/§4；supply-chain-inventory-development-design §6/§12 | 已覆盖 | 已新增供应链开发设计，覆盖供应商、供应合同、采购周期、库房、库位、库存流水和 P0 编码任务。 |
| REQ-28 | **供应合同三模式：平台自用/商户自采/平台统一采购分销** | supply-chain PRD；business-decision-log §6；supply-chain-inventory-development-design §6.2 | 已覆盖 | 供应合同模式已落为 `contract_mode` 口径，支持 platform_self_use、merchant_self_purchase、platform_purchase_distribution。 |
| REQ-29 | P0 供应链从酒类/吧台消耗/客人存酒/常用消耗品开始 | business-decision-log；supply-chain PRD §P0 范围 | 已覆盖 | 常用消耗品=纸巾/色盅/色子/杯子 |
| REQ-30 | **客人存酒用标签（优先瓶底），按标签数值/拍照/人工备注记录** | bar PRD §4；business-decision-log §16；bar dev-design §6.11 saved_bottles | 已覆盖 | `saved_bottles` 已补 `bottle_label_value`、`customer_name_or_nickname`、`registration_photo_refs`、`manual_note`，登记必填并用于取酒核身。 |
| REQ-31 | 存酒需留客人手机号和姓名/昵称 | bar PRD §4；bar dev-design §6.11 | 已覆盖 | `customer_phone` 与 `customer_name_or_nickname` 均已落表，散客无会员时可用于核身。 |
| REQ-32 | **高价值报损/盘点差异/存酒作废必须拍照和视频留证 + 主管复核** | business-decision-log §16；bar dev-design §5.4.4/§5.4.5/§5.3.4；supply-chain-inventory-development-design §5.4/§6.11 | 已覆盖 | 高价值判定已落 `is_high_value/high_value_threshold` 与平台默认阈值，报损/作废/盘点触发照片+视频双证据和主管复核。 |
| REQ-33 | 人工放行/免单/退款/调账按可配置层级主管复核 | platform-governance PRD §12；business-decision-log §25/§26 | 已覆盖 | 审批阶梯字段见 REQ-58 |

## 六、全业务过程留痕（original §9）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-34 | **留痕不只物品交接：凡有过程的业务动作都必须留痕** | operation-traceability PRD §2（唯一权威）；business-decision-log §31；p0-action-event-evidence-matrix-v0.1 | 已覆盖 | 三层模型 business_process_instances/step_records/service_responsibility_records（trace 覆盖小结）；P0 事件证据矩阵补齐“每个关键动作留什么证据”。 |
| REQ-35 | 订单获取/销售归属留痕（销售是谁、谁下单） | operation-traceability PRD 2.3/2.4；java-backend 事件 `order.acquired`/`order.sales_attributed` | 已覆盖 | 事件命名以 PRD 4.1 为唯一权威（D4） |
| REQ-36 | 卡座服务留痕（谁服务、加单、送达、换人） | operation-traceability PRD 2.4 服务责任链/6.6 | 已覆盖 | — |
| REQ-37 | 异常第一处理人留痕 + 转派/升级/复核/关闭/退款/免单/调账 | operation-traceability PRD 2.4 异常/财务责任链；foundation-database-design §6.12；API first-handle | 已覆盖 | `service_responsibility_records` 已补 `refunded_by`、`waived_by`、`approved_by` 财务责任段字段。 |
| REQ-38 | 酒从库房→订单核验→机器人/人工配送→后厨→服务员送达全链路扫码 | operation-traceability PRD §3.2 流程/§3.3 节点 | 已覆盖 | — |
| REQ-39 | 酒入库贴码或出库打印码，二维码串责任链 | operation-traceability PRD §3.1/§6.1 inventory_trace_codes | 已覆盖 | — |
| REQ-40 | **优先自动留痕、尽量避免人工填写**，人工只补原因/照片/视频/签名 | operation-traceability PRD §2.2/§6.5；foundation-database-design §6.11；p0-action-event-evidence-matrix-v0.1 | 已覆盖 | 已补 `capture_source` 来源类型，区分扫码、支付回调、机器人、企微、第三方、MDM、系统规则和人工补录，可统计自动留痕率；矩阵明确高风险动作证据要求。 |
| REQ-41 | 留痕与监控视频后续一一对应，P0 不强依赖真实视频 | operation-traceability PRD §2.5 视频关联策略 | 已覆盖 | P0 仅留时间/点位/设备推导候选 |
| REQ-42 | **合同与准入也必须留痕** | operation-traceability PRD 2.3「合同和准入」+ **6.4 process_type `contract_admission`（2026-06-07 已补）** + 4.1 `contract.blocked`/`contract.released`（已补） | 已覆盖 | trace-3 已修复 |

## 七、机器人与配送（original §10）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-43 | 酒从库房倾向机器人配送，**真实调度/厂家联动可后置** | operation-traceability PRD §8（机器人真实状态回调=P1，调度=P2） | 已裁决并覆盖 | **pboundary-4 已收口：android 原标 P1 联动已统一改 P2；交接扫码、责任链和状态留痕=P0**（见 p0-implementation-task-list §2） |
| REQ-44 | P0 必须记录机器人或人工配送交接留痕、各环节扫码 | operation-traceability PRD §3.3；business-decision-log §29 | 已覆盖 | — |

## 八、游乐系统（original §11）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-45 | 游乐可独立成乐园 SaaS | amusement PRD；operation-traceability PRD §5 | 已覆盖 | — |
| REQ-46 | **游乐容量 P0 按具体项目 + 具体场次/时段 + 点位/设备控制（三维度）** | business-decision-log §6；amusement dev-design §7.4/§4.2/§7.14b/§7.14c；amusement-p0-safety-inspection-spec-v0.1 §5/§9 | 已覆盖 | 点位/设备容量字段、`amusement_capacity_config` 和 `amusement_capacity_snapshot` 已补；核销必须同时校验安全状态与容量。 |
| REQ-47 | 一体机查看游乐开放状态/容量/异常/检查 | amusement PRD；terminal PRD；amusement-p0-safety-inspection-spec-v0.1 §5/§6 | 已覆盖 | P0 开放状态应包含安全巡检通过、警告、暂停、待恢复等可行动状态。 |
| REQ-48 | **游乐定期检修、检修单位作为供应商、工程师一体机填写、主管确认后恢复营业** | amusement dev-design §6.6.1/§7.14a；business-decision-log §32 引用；amusement-p0-safety-inspection-spec-v0.1 §6.3/§9 | 已覆盖 | 已补 `amusement_maintenance_record` 和 P0 安全巡检闭环；严重项整改复检通过后，仍必须主管审批恢复营业。 |

## 九、一体机（original §12）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-49 | 一体机触摸屏，可下单；并区分售票型与非售票型，售票型承担官方自助购票/取票/订单查询，非售票型承担订台/吧台买酒/权益领取/游乐状态/检修记录 | unified-standards-baseline D5；terminal-page PRD；android-device-terminal-development-design；ticketing-pass-entitlement-development-design；p0-end-to-end-acceptance-and-edge-cases §2 | 已覆盖 | 终端开关枚举仍为 `kiosk`，用 `kiosk_mode=ticketing/service` 或模块开关区分，不新增第 11 端；售票型必须复用票务价格、库存锁定、聚合支付、出票和防伪二维码 |
| REQ-50 | 一体机订台也需定金 | business-decision-log §5；bar dev-design §6.3/§13.1 | 已裁决并覆盖（P0） | 同 REQ-25：bar-operations §6.3/§13.1 已裁决订台+定金为 P0，定金优先级冲突已消除 |
| REQ-51 | 一体机动作记录设备/点位/时间/结果 | operation-traceability PRD §3.3/6.5 | 已覆盖 | — |

## 十、工作手机/私人手机/企业微信（original §13）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-52 | 工作手机核销门票（扫客人 App 二维码） | android-device-terminal dev-design §1/§2/§4 | 已覆盖 | — |
| REQ-53 | 工作手机扫附赠权益二维码→一体机展示可领取 | android dev-design §5.5/§14.5 | 已覆盖 | 已补工作手机扫码→指定/就近一体机展示、`entitlement_session_id` 会话关联、会话时效和防错领机制。 |
| REQ-54 | **企微每日上传聊天、服务分析、违规检查；原始提出永久保存/不脱敏，现行裁决为可配置留存/分级查看** | business-decision-log §27；data-risk-ai dev-design §11.2/§11.5；compliance-and-personal-information-protection-requirements §4 | 已裁决并覆盖 | 原“永久保存”已按 A7/C3 改为可配置留存期限；普通展示按权限脱敏；查看不脱敏原文仅限最小授权+审批的取证/质检场景，且必须写访问审计。原始需求保留，执行口径以合规文档与 D8/D10 基准为准。 |
| REQ-55 | 工作手机禁私装软件、仅企业微信、白名单网站，违规 MDM 阻止/封禁权限并留痕 | android dev-design §13.1/§13.4；android-terminal-breakdown §8.4；business-decision-log §14；compliance 要求 | 已覆盖（方案已拍板） | 封禁触发条件、状态机、解封流程已补；执行方案已确认为 Android Enterprise / Android Management API + 商业 MDM，由平台 IT 选型、采购、配置和运维。 |
| REQ-56 | **私人手机 App 注册/通知/状态/设备凭证绑定/Root 检测，不做现场业务** | unified-standards-baseline **D8**；business-decision-log **§32 A6**；android dev-design §3/§9 | 已裁决并覆盖 | **phone-3 已裁决：原"读手机本机 NFC 唯一标识"Android 技术不可行；已改为设备指纹/Keystore 设备公钥凭证（D8/A6）；仅存不可逆哈希（compliance-4/phone-7）**。工作手机通过摄像头/扫码模块读取外部二维码，NFC 仅读取外部 NFC 腕带、标签、卡或设备中的权益标识。 |
| REQ-57 | 换机先解绑旧再绑新；Root/调试/异常判不可信失效复核 | business-decision-log §32 A6；android dev-design | 已覆盖 | 绑定对象由"手机 NFC"改为"设备凭证"（D8） |
| REQ-57b | **员工进办公区私人手机寄存，寄存柜流动使用、先到先选、离开拿走** | business-decision-log §11/§32 引用；android dev-design §3/§9/§13；project-progress-dashboard §6.1 已确认 | 已覆盖 | 已确认规则：不绑定固定柜，先到先选，离开全部拿走；P0 可人工记录/确认寄存与取走，P1 可接门禁/储物柜/考勤。 |

## 十一、员工、权限、职级与薪资（original §14）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-58 | **权限单一功能最小颗粒度（一个功能=一个权限点）** | foundation-database-design permissions 表；java-backend access 模块；business-decision-log §26 | 已覆盖 | 示例 ticket.verify.execute（权限编码 dot 风格，D3） |
| REQ-59 | 权限完全自定义（可增可减），可按角色、模板、员工单独配置 | foundation-database-design §3.4-3.13/§3.13.1-3.13.5；p0-operation-permission-matrix-v0.1；unified-standards-baseline D11 | 已覆盖 | 已补 `user_permission_overrides` 和权限模板表组，支持基础模板、临时模板、复制模板、单次有效和 `grant/deny`；最终权限按 D11 计算。 |
| REQ-60 | **职级 M1-M10 并分上/中/下** | business-decision-log §32 引用；platform-governance field-dictionary §8.1/§9.1；foundation-database-design §3.2.1 | 已覆盖 | `staff_levels` 已拆 `main_level`、`sub_level` 并派生 `level_rank` 1..30，支撑 M1-M10 × 上中下完整序列。 |
| REQ-61 | 职级只给薪资大范围，**具体薪资单独填写** | platform-governance field-dictionary §9.1；foundation-database-design §3.2 | 已覆盖 | 已补 `base_salary_amount`、`salary_currency`、`salary_detail`，职级只提供 `salary_policy_summary` 大范围。 |
| REQ-62 | 职级影响默认权限模板与审批层级，但不能替代单一功能权限 | business-decision-log §26；product-requirements §12 已确认规则 30；platform-governance PRD §12；platform-governance field-dictionary §8.1/§9.1；p0-operation-permission-matrix-v0.1 | 已覆盖 | 已在总纲和平台治理 PRD 补强禁止性约束：职级只影响默认模板、审批层级建议和薪资大范围，不能替代单一功能权限点，不能绕过具体功能授权。 |
| REQ-63 | 员工私人手机 App 注册提交→后台审核通过→生成角色 | platform-governance PRD 员工注册审批 | 已覆盖 | — |

## 十二、平台治理、合同与准入（original §15）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-64 | 基础平台管综合体/商户/合同/空间/区块/设备/人员/权限/终端开关 | platform-governance PRD §5；platform-governance field-dictionary §5/§7/§8；operation-traceability PRD §5 | 已覆盖 | 商场区块已作为楼层与区域之间的独立治理对象落到 PRD 与字段字典；P0 做区块层级，铺位级精细管理后置 P1。 |
| REQ-65 | 商户信息（合同审批/面积/租赁面积/商场区块/负责人/两个联系人/每平米租金/合作形式） | platform-governance PRD §5；field-dictionary | 已覆盖 | — |
| REQ-66 | 商场区块最好有平面图 | platform-governance PRD；platform-governance field-dictionary §5；business-module-mature-system-gap-analysis §7.2 | 已裁决并覆盖（P1 预留） | 平面图来自用户原始需求；P0 保留区块/铺位与经营面积字段，平面图上传、多边形标注和铺位可视化后置 P1，避免阻塞 P0。 |
| REQ-67 | 合同核心摘要/基础定义由平台治理管理 | platform-governance PRD §5/§7 | 已覆盖 | — |
| REQ-68 | **合同/基础定义违规全部先卡住，允许更高级审核后放行** | platform-governance PRD §12；business-decision-log §25；operation-traceability PRD `contract.blocked`/`contract.released` | 已覆盖 | — |
| REQ-69 | **审批层级逐级上提（上一级/上两级/最高权限）** | platform-governance PRD §12；platform-governance field-dictionary §8.1；foundation-database-design §3.2.1.1/§2.9.2 | 已覆盖 | 已定义 `one_level_up/two_levels_up/top_authority/specific_role`，按 `level_rank` 计算，并在 `admission_audits.approval_level` 留痕。 |

## 十三、第三方接入（original §16）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-70 | 第三方资料未定，先做自有系统再接入；分"获取/回写"与"接 SDK/API"两类 | third-party-integration dev-design；third-party-supplier-integration-kit-v0.1；business-decision-log §28；unified-standards-baseline D3 | 已覆盖 | 开放路径/权限编码以 D3 为准；供应商未确定前只保留接口、适配器、字段映射占位、Webhook+轮询校验、对账和补偿。 |
| REQ-71 | 餐饮/零售同时支持订单级和券核销级回流 | third-party PRD；business-decision-log §6 | 已覆盖 | — |
| REQ-72 | 看板支持日/半日/小时汇总，单店小数据量可实时 | data-analysis-layer PRD；business-decision-log §6 | 已覆盖 | — |
| REQ-73 | 美团/大众点评/抖音 P0 只做团购购票/回流/核销/对账 | business-decision-log §2；ticketing PRD | 已覆盖 | 同 REQ-03 |

## 十四、终端开关与 SaaS 化（original §17）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-74 | **所有端都要有开关接口（10 端含移动管理端）** | unified-standards-baseline **D5**；operation-traceability PRD §5.2/§6.3 | 已覆盖 | 10 端权威表 terminal_type；govfam-3 跨文档枚举对齐以 D5 为准 |
| REQ-75 | 关闭后菜单不可见/不可用，**接口拒绝业务动作并留痕** | operation-traceability PRD **§5.4（2026-06-07 补后端拒绝+`terminal.access_rejected` 留痕）** | 已覆盖 | switch-4 已修复 |
| REQ-75b | 开关拉取失败兜底 | operation-traceability PRD **§5.6 fail-closed（2026-06-07 新增）** | 已覆盖 | switch-5 已补 |
| REQ-76 | 终端开关用于酒吧/乐园未来 SaaS 化 | operation-traceability PRD §5.1/§9 | 已覆盖 | — |
| REQ-77 | **商户侧在授权业务范围内全开放** | operation-traceability PRD **§5.7 优先级裁决（2026-06-07 新增）**；business-decision-log | 已覆盖 | switch-6：高层级开关关闭压倒"全开放"，二者不矛盾；htmlmap-1 业务 map 卡片"只读"旧口径需改 |

## 十五、Web 前端与移动端（original §18）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-78 | Web 前端供业务人员非编辑查看，**P0 允许少量业务确认动作** | web-mobile-app-terminal-extension PRD；business-decision-log | 已覆盖 | — |
| REQ-79 | 客人 App/小程序：购票/预定/查询/出示二维码/会员积分/兑换 | customer-app-miniapp-development-design §4/§8；terminal PRD；web-mobile-app PRD；foundation-database-design §6B.5 | 已覆盖 | 会员积分和兑换下游表已建模（`points_accounts`、`points_ledger`、`points_rules`、`point_gifts`、`point_redemptions`），客人端页面与接口依赖已显式列出。 |
| REQ-80 | 移动管理端给主管看指标/待办/异常审批 | terminal PRD；terminal_type=mobile_admin（D5） | 已覆盖 | — |

## 十六、数据分析、风控与 AI 质检（original §19）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-81 | 看板看收入/客流/核销/退款/异常/商户表现/对账差异/库存/供应链/回流/企微质检 | data-analysis-layer PRD §4/§7 | 已覆盖 | — |
| REQ-82 | **销售服务分析：订单来源/销售归属率/卡座服务责任完整率/送达确认率/异常首响时长** | data-risk-ai dev-design §5.9；data-analysis-layer page-list §7.3；field-dictionary §6.1；acceptance-checklist §3/§4 | 已覆盖 | 四项核心指标和服务责任变更次数已补分子分母、来源表、metric_code、页面、下钻和验收；指标名统一为"卡座服务责任完整率"。 |
| REQ-83 | 风控覆盖异常核销/库存/财务差异/企微违规/服务质量/设备策略违规 | data-risk-ai dev-design §10/§13.4/§13.5；evidence-risk-control PRD | 已覆盖 | `risk_rules.domain` 已补 `order/service/exception`；`risk_alerts.object_type` 已补 `table_session/service_responsibility`，责任链风控可准确下钻。 |
| REQ-84 | **AI 深度分析/语义质检/经营预测可后置** | unified-standards-baseline（P2/P3 口径）；p0-implementation-task-list §2 | 已裁决并覆盖 | **pboundary-1 已收口：企微 AI 质检及下游 AI 分析统一 = P2（消除 P2/P3 错位）；P3=AI 增强占位**。P0 只做基础规则质检、留痕和人工复核入口。 |

## 十七、文档与交付要求（original §20）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-85 | 生成可供分析判断的完整系统文档、可直接用于编码 | system-development-spec；system-capability-layers-product-index | 已覆盖 | — |
| REQ-86 | 文档按模块拆解，颗粒度足够编码 | 各 *-development-design；各层五件套；p0-implementation-task-list | 已覆盖 | P0/P1/P2 边界以 `p0-implementation-task-list.md` 与本矩阵收口状态为准；历史审计中的边界提示已在本轮产品流复核中更新。 |
| REQ-87 | **业务人员版 HTML 可点击说明页（给不懂技术的人看）** | business-system-map-for-operations.html | 已覆盖 | htmlmap-3：HTML 未标生成时间与依据 PRD 版本 |
| REQ-88 | **平台治理也需业务人员 HTML 拓扑页** | platform-governance-map-for-operations.html | 已覆盖 | xref-9：已补入 system-capability-layers 索引 §2 |
| REQ-89 | **所有项目内容需打包，方便发给其他机器智能体** | **delivery-package-manifest-v0.1.md（2026-06-07 新建）** | 已覆盖 | deliver-1 已修复，见交付包说明 |
| REQ-90 | 需求→文档逐条可追溯 | **本文档 requirement-traceability-matrix-v0.1.md（2026-06-07 新建）** | 已覆盖 | deliver-2 已修复 |

## 十八、多综合体与集团视图（original §2）

| 需求编号 | 原始口径摘要 | 承接文档 + 章节 | 覆盖状态 | 备注 |
|---|---|---|---|---|
| REQ-91 | P0 围绕单综合体闭环，所有核心对象预留综合体项目归属 | platform-governance PRD；foundation-database-design | 已覆盖 | — |
| REQ-92 | 后续支持多综合体和集团视图，**优先级低** | product-requirements §12 已确认规则 27；operation-traceability PRD §8（集团级 P2）；多处治理文档 | 已裁决并覆盖（P2 预留） | P0 围绕单综合体闭环并保留 `project_id`；集团视图和跨项目汇总统一后置 P2，不阻塞当前编码。 |

## 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-06-07 | 增 | 新建本文档 | 为 original-requirements 每条口径编 REQ-01~REQ-92，逐条映射承接文档+章节、覆盖状态、备注，重点覆盖易漏细则（员工不可代购票、二维码刷新秒数、职级 M1-M10 上中下、企微留存、寄存柜流动、现金 POS、存酒瓶底标签等） | finding deliver-2、deliver-1；口径以 unified-standards-baseline 与 business-decision-log §32 为准 |
| 2026-06-07 收口 | 改 | REQ-22 | "部分覆盖·缺字段"更新为"已覆盖"，指向 bar dev-design §6.6/§6.16 已落地的按杯字段 | bar 已新增 standard_pour_volume 等字段，矩阵结论过时。依据 finding broken-ref-6 |
| 2026-06-07 收口 | 改 | REQ-25、REQ-50 | 订台定金覆盖状态由"有待对齐"更新为"已裁决（P0）"，指向 bar-operations §6.3/§13.1 | 本轮 bar 已裁决订台+定金为 P0，矩阵注解滞后。依据 finding newdocs-2 |
| 2026-06-08 Product Design 复核 | 改 | REQ-05/06/11/25/43/50/55/56/57b/62/64/66/79/82/83/84/92 | 将已修复但状态滞后的需求行更新为已覆盖、已裁决并覆盖或 P1/P2 预留；补 ticketing PRD §6.1 和 product-requirements §12 规则 30 | documentation-product-flow-audit-and-fix-report-20260608：旧审查状态未同步会误导外部智能体 |
| 2026-06-10 产品审计 V0.6 | 改 | REQ-49 | 补“一体机分售票型/非售票型，售票型承担官方自助购票/取票/查单”的追溯锚点 | 用户确认一体机有售票和不售票两种，售票型需承担售票职责 |
| 2026-06-10 卡座销售提成 | 增 | REQ-26b | 新增销售人员岗位和卡座提成追溯锚点 | 用户新增卡座销售提成规则 |
