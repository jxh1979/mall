# P0 端到端验收标准与边界用例 V0.1

> 本文档把关键业务闭环写成 Given/When/Then 验收标准和边界用例。它用于开发自测、测试用例设计、上线验收和供应商联调准入。

## 1. 验收范围

P0 必须覆盖以下闭环：

1. 票务价格、购票、支付、出票、动态码、核销、退款、对账。
2. 通票权益、酒水领取、平台承担赠酒成本、留痕和财务归集。
3. 卡座预订入场人数权益、只入预定酒吧、不抵扣最低消费。
4. 酒吧年龄/人脸核验、开台、点酒、出酒、扫码支付、日结。
5. 客人存酒、领取、作废、证据留痕。
6. 高价值酒水库存扣减、盘点差异、报损和审批。
7. 游乐基础项目、场次、容量、核销、人工放行、开放前安全巡检、整改复检和主管恢复审批。
8. 完整应收应付台账、四类主体唯一绑定、结算主体留痕。
9. 权限模板、临时授权、单次有效和审批留痕。
10. 第三方双向接入：平台开放接口 + 供应商 SDK/API 适配占位。

## 2. 票务与权益

### AC-TKT-01 基础价与派生价

Given 管理员配置平日、周末、节假日基础票价  
When 创建多人票、新人票、学生票或渠道价  
Then 系统必须支持按基础价规则生成价格，或由授权人输入固定价格策略  
And 发布前必须经过价格审批并写入 `ticket_price.published`。

### AC-TKT-02 动态二维码核销

Given 游客已购票并打开 App/小程序二维码  
When 员工工作手机或授权核销设备扫码核销  
Then 系统必须在线校验签名、有效期、随机串、票种资格和权益状态  
And 成功后写入 `verification.succeeded`，失败写入 `verification.failed`。

### AC-TKT-02A 售票型一体机自助购票

Given 售票型一体机已绑定点位、设备编号、`kiosk_mode=ticketing` 且票务模块开关打开  
When 客人选择票种并完成聚合支付  
Then 系统必须按平日/周末/节假日基础价及其派生价校验价格  
And 必须先锁定库存再创建订单和支付二维码  
And 支付成功后生成票实例、权益实例和防伪二维码  
And 写入 `kiosk.ticket.order_created`、`payment.gateway_notified`、`ticket.issued`  
And 非售票型一体机不得出现购票入口。

### AC-TKT-03 退款与冲正

Given 订单已支付且部分权益未使用  
When 客服发起退款  
Then 系统必须按订单和权益状态计算可退范围  
And 审批通过后调用聚合支付退款或生成线下退款凭证  
And 已核销权益不得直接删除，只能生成冲正/退款事件。

### 边界用例

| 场景 | 预期 |
|---|---|
| 周末跨节假日 | 按节假日优先级覆盖周末 |
| 学生票未通过人工审核 | 不能支付或不能出票，按配置控制 |
| 多人票只到一部分人 | P0 必须同一时段集中核销，不支持拆到不同日期/场次 |
| 截图二维码 | 动态码过期后拒绝 |

## 3. 酒吧、通票赠酒、卡座与最低消费

### AC-BAR-01 通票赠酒

Given 游客持有包含酒水权益的通票  
When 吧台核销赠酒  
Then 系统必须扣减对应权益并记录领取人、吧台员工、时间和终端  
And 赠酒成本归平台承担，不计入酒吧商户应收成本。

### AC-BAR-02 卡座入场人数权益

Given 客人预订某酒吧某卡座，卡座配置允许免通票入场人数  
When 门口员工为客人入场  
Then 系统只能在该卡座所在酒吧放行  
And 入场人数不得超过该卡座配置额度  
And 不得允许进入其他酒吧  
And 该免通票入场权益不得抵扣最低消费。

### AC-BAR-02A 卡座入场名额并发与幂等

Given 某卡座预约剩余免通票入场名额为 1  
When 两台设备几乎同时提交入场名额核销  
Then 系统只能成功扣占 1 人名额并写入 `bar.table_guest.checked_in`  
And 另一笔必须拒绝并写入 `bar.table_guest.rejected`  
And 同一 `idempotency_key` 重试必须返回第一次处理结果，不得重复扣占。

### AC-BAR-03 最低消费

Given 卡座或时间段配置了最低消费  
When 酒吧结账  
Then 系统只按酒吧账单消费判断是否达到最低消费  
And 票务、通票、卡座入场权益不得抵扣最低消费。

### AC-BAR-04 年龄与人脸核验

Given 客人进入酒吧售酒场景  
When 进行年龄核验  
Then 系统必须要求第三方人脸/年龄核验接口或等效设备返回通过结果  
And 正式生产不得仅靠人工口头确认标记为通过  
And 供应商未确定前只保留接口、字段占位和测试模拟。

### AC-BAR-05 卡座销售提成

Given 某卡座订单已绑定销售归属员工  
When 该卡座账单结清并进入日结  
Then 系统必须按酒吧账单实收可计提消费额生成卡座提成记录  
And 销售归属员工岗位为销售人员时提成率为 15%  
And 销售归属员工岗位不是销售人员时提成率为 10%  
And 提成基数不得包含通票、票务权益、卡座预约入场名额、赠酒成本、退款、免单、抹零和不可计提调整  
And 写入 `bar.table_commission.calculated`；日结后变更只能写调整/冲正事件。

### 边界用例

| 场景 | 预期 |
|---|---|
| 卡座预订 A 酒吧，客人去 B 酒吧 | 拒绝入场，提示仅限预定酒吧 |
| 卡座人数额度已满 | 拒绝免通票入场，可引导正常购票 |
| 卡座剩余名额被并发占用 | 以服务端锁定/版本号结果为准，只允许一笔成功 |
| 通票赠酒已领取 | 再次领取拒绝并显示已核销时间 |
| 年龄人脸核验接口超时 | 不得自动放行，可转主管人工异常流程并留证 |
| 销售归属员工岗位变更 | 提成按订台/开台时的岗位快照计算；变更需主管复核 |
| 卡座账单后续退款或免单 | 生成提成冲正，不覆盖原提成记录 |
| 同一卡座多人争抢销售归属 | P0 只允许一个主销售归属；复杂多人分摊进入 P1 |

## 4. 存酒与库存

### AC-INV-01 高价值酒水库存扣减

Given 吧台出酒或客人领取存酒  
When 操作完成  
Then 系统必须在线写入库存扣减或存酒状态变更  
And 高价值酒水不得离线完成核销或扣减。

### AC-INV-02 存酒登记与领取

Given 客人要求存酒  
When 吧台登记  
Then 系统必须记录瓶底标签、照片、手机号/昵称、员工和备注  
When 客人领取  
Then 必须记录领取签收和状态变更。

### AC-INV-03 盘点与报损

Given 库存盘点出现差异或高价值酒水报损  
When 发起调整  
Then 必须上传照片/视频或附件  
And 主管/财务复核后才影响库存和财务口径。

### 边界用例

| 场景 | 预期 |
|---|---|
| 出酒成功但库存扣减失败 | 订单进入异常，禁止日结通过，需补偿 |
| 同一瓶存酒被重复领取 | 第二次拒绝并提示领取记录 |
| 盘点后直接改库存 | 禁止，必须走调整审批 |

## 5. 支付、应收应付与日结

### AC-FIN-01 聚合支付与直连接口抽象

Given P0 选择聚合支付  
When 发起扫码支付、退款或对账  
Then 系统通过支付抽象层调用聚合支付  
And 保留直连支付接口抽象但不默认启用。

### AC-FIN-02 应收应付台账

Given 票务、酒吧、供应链、合同或第三方对账产生金额义务  
When 业务单据确认  
Then 系统必须生成应收或应付台账记录  
And 台账必须绑定经营主体、运营主体、合同主体、结算主体及来源单据。

### AC-FIN-03 日结锁定

Given 当日业务已完成对账  
When 财务锁定日结  
Then 锁定后不得直接修改影响金额的原始记录  
And 后续更正只能通过补录、冲正或调整单完成。

### 边界用例

| 场景 | 预期 |
|---|---|
| 聚合支付回调重复 | 幂等处理，只认一笔有效结果 |
| 回调成功但轮询状态不一致 | 标记差异，进入对账补偿 |
| 金额币种非 CNY | P0 拒绝或进入异常复核，不自动换算 |
| 日结后补录线下收款 | 生成补录事件，不覆盖原日结 |

## 6. 权限模板

### AC-AUTH-01 基础模板复制

Given 系统已有“普通员工”基础模板  
When 管理员复制为“普通员工01”并修改部分权限  
Then 新模板应保留源模板快照并独立维护  
And 源模板后续变更不自动影响“普通员工01”。

### AC-AUTH-02 临时模板时效

Given 临时模板设置有效期为 2026-07-01 18:00 至 2026-07-02 02:00  
When 员工在有效期内操作  
Then 权限生效  
When 超过有效期再次操作  
Then 权限失效并写入到期事件。

### AC-AUTH-03 单次有效

Given 临时模板设置为单次有效  
When 员工完成一次授权动作  
Then 模板应用记录应变为已消耗  
And 第二次同类动作必须拒绝。

## 7. 第三方接入

### AC-OPEN-01 双向开放

Given 第三方需要调用平台能力  
When 第三方申请开放应用  
Then 平台通过统一 OpenAPI、鉴权、签名、Webhook 提供能力  
And 路径必须采用 `/api/open/d/{endpoint_key}/...`。

### AC-OPEN-02 供应商 SDK/API 适配

Given 平台需要接入聚合支付、企业微信、机器人、视频、设备、人脸等供应商  
When 供应商 SDK/API 尚未确定  
Then 文档只保留统一适配接口、字段映射占位、Webhook/轮询策略和异常补偿规则  
And 不得把供应商私有字段写死进业务主模型。

### AC-OPEN-03 Webhook + 轮询校验

Given 供应商支持 Webhook 和查询接口  
When 供应商回调业务结果  
Then 系统先按 Webhook 实时处理  
And 定时轮询校验关键状态  
And 差异进入对账/补偿队列。

## 8. 游乐基础运营

### AC-AMU-01 场次容量

Given 游乐项目配置场次、时段、点位和容量  
When 游客核销权益  
Then 系统必须校验项目、场次、时段和剩余容量  
And 成功后扣减容量并写留痕。

### AC-AMU-02 人工放行

Given 游乐核销失败  
When 主管人工放行  
Then 必须填写原因、上传证据并审批  
And 原失败记录与放行记录可追溯。

### AC-AMU-03 开放前安全巡检

Given 某游乐项目当天没有有效开放前安全巡检通过记录  
When 员工尝试开放该项目或执行游乐核销  
Then 系统必须拒绝并提示“未完成开放前安全巡检”  
And 拒绝记录必须写入事件中心。

### AC-AMU-04 严重项恢复开放

Given 某游乐项目巡检存在阻断项或重大项  
When 运营人员提交整改并完成复检  
Then 系统仍不得自动恢复开放  
And 只有具备恢复审批权限的主管审批后，项目才可重新开放和核销。

### AC-AMU-05 安全状态与容量同时校验

Given 某游乐项目安全巡检已通过但场次容量已满  
When 游客继续核销权益  
Then 系统必须按容量不足拒绝核销  
And 安全通过不得绕过容量控制。

## 8A. 高风险 operation 验收映射

本节用于把端到端验收标准和 OpenAPI operationId 直接绑定，避免开发、测试或供应商只看到业务验收描述但无法定位接口。P0 不要求 181 个 operation 全量逐条写 Given/When/Then；但以下高风险闭环必须能按 AC 编号、路径和 operationId 追溯。

| 验收项 | 高风险闭环 | 必须覆盖的 OpenAPI 路径 / operationId | 验收底线 |
|---|---|---|---|
| AC-TKT-02 | 动态二维码核销 | `POST /api/v1/ticketing/verifications/preview` / `previewTicketVerification`；`POST /api/v1/ticketing/verifications` / `createTicketVerification` | 预览和正式核销均校验票、权益、签名、有效期、终端和幂等；失败必须有 `verification.failed` |
| AC-TKT-02A | 售票型一体机购票 | `GET /api/v1/terminals/kiosks/{device_id}/config` / `getKioskConfig`；`POST /api/v1/ticketing/orders` / `createTicketOrder`；`POST /api/v1/ticketing/orders/{order_id}/pay-intents` / `createTicketOrderPayIntent`；`POST /api/v1/ticketing/orders/{order_id}/issue` / `issueTicket` | 只有 `kiosk_mode=ticketing` 可售票；支付成功后才出票和发权益 |
| AC-TKT-03 | 退款与权益冲正 | `POST /api/v1/ticketing/refunds` / `requestTicketRefund`；`POST /api/v1/ticketing/refunds/{refund_id}/approve` / `approveTicketRefund`；`POST /api/v1/finance/refunds` / `createFinanceRefund` | 已核销权益不得删除，只能退款、撤销、冲正并留痕 |
| AC-BAR-02 | 卡座免通票入场名额 | `POST /api/v1/bar/reservations/{id}/check-in-guest` / `checkInBarReservationGuest` | 只能进入预定卡座所在酒吧；不得超过人数额度；不得抵扣最低消费 |
| AC-BAR-04 | 酒吧年龄/人脸核验 | `POST /api/v1/bar/age-verifications` / `createBarAgeVerification` | 第三方或等效设备未通过时不得生产放行；人工复核只能走异常留痕 |
| AC-BAR-05 | 卡座销售提成 | `POST /api/v1/bar/bills/{bill_id}/commissions/calculate` / `calculateBarTableCommission` | 销售人员 15%、非销售人员 10%；提成基数排除通票、权益、赠酒、退款、免单和不可计提调整 |
| AC-INV-01 | 高价值酒水库存扣减 | `POST /api/v1/inventory/stock-movements` / `createStockMovement`；`POST /api/v1/inventory/stock-movements/{movement_type}` / `createTypedStockMovement` | 高价值酒水不得离线扣减；失败必须阻断日结或进入补偿 |
| AC-INV-02 | 存酒与取酒 | `POST /api/v1/inventory/saved-bottles` / `createSavedBottle`；`POST /api/v1/inventory/saved-bottles/{id}/claim` / `claimSavedBottle` | 存酒、取酒、作废均必须有客户、员工、证据和状态变更 |
| AC-FIN-01 | 聚合支付与直连抽象 | `POST /api/v1/finance/payment-intents` / `createPaymentIntent`；`POST /api/v1/bar/bills/{bill_id}/pay-intents` / `createBarBillPayIntent`；`POST /api/v1/ticketing/orders/{order_id}/pay-intents` / `createTicketOrderPayIntent` | P0 默认聚合支付；直连只保留抽象，不默认启用 |
| AC-FIN-02 | 完整应收应付台账 | `GET /api/v1/finance/ar-ap-items` / `listArApItems`；`POST /api/v1/finance/ar-ap-items` / `createArApItem`；`POST /api/v1/finance/ar-ap-items/{id}/payments` / `createArApPayment` | 台账必须绑定来源单据、四类主体和收付款记录 |
| AC-OPEN-01 / AC-OPEN-03 | 双向开放与 Webhook + 轮询校验 | `POST /api/open/d/{endpoint_key}/webhooks/{provider}/{event}` / `receiveOpenWebhook` | Webhook 要验签、幂等、防重放；轮询用于校验关键状态和补偿差异 |
| 生产前置-MDM | 设备纳管与终端限制 | `POST /api/v1/terminals/devices/bind` / `bindDevice`；`POST /api/v1/terminals/sync-tasks` / `syncTerminalTasks`；`PUT /api/v1/terminals/capability-switches/{id}` / `updateCapabilitySwitch` | 未纳管设备不得生产使用；高风险终端操作必须按设备凭证和能力开关拦截 |

## 9. 上线前阻断项

以下阻断项不是新的产品拍板问题。自有系统可以先按统一接口、mock、适配器和配置占位开发；但未满足通过标准前，不得进入真实生产或把对应能力标记为生产就绪。

| 阻断项 | 通过标准 |
|---|---|
| MDM/Android Enterprise 或商业 MDM | 工作手机/平板纳管、策略下发、丢失处置、应用限制可用 |
| 聚合支付 | 支付、退款、回调、轮询、对账、异常补偿联调通过 |
| 酒吧人脸/年龄核验 | 供应商接口或设备采购完成，生产环境可用 |
| NAS 备份 | 定时备份、恢复演练、权限隔离完成 |
| 权限模板 | 基础、复制、临时、单次、审批、留痕通过 |
| 应收应付 | 全业务来源单据能生成台账并与日结关联 |
| 游乐安全巡检 | 巡检模板、开放前阻断、整改复检、主管恢复审批和核销安全状态校验通过 |

## 10. 生产前置责任清单

以下事项不是新的产品待决策问题，也不影响自有系统按统一接口、mock、适配器和配置占位进入 P0 开发。但在未完成对应验收前，不得进入真实生产，不得对外宣称对应能力生产就绪。

| 前置项 | 默认方案 | 责任部门 | 是否阻断真实生产 | 自有系统可先开发内容 | 生产通过标准 | 未通过时系统处理 |
|---|---|---|---|---|---|---|
| MDM / Android Enterprise / 商业 MDM | Android Enterprise / Android Management API + 商业 MDM 组合 | 平台公司 IT | 是 | 设备绑定、设备凭证、终端权限、策略状态字段、未纳管拦截逻辑 | 工作手机、平板、一体机完成纳管；支持策略下发、应用限制、丢失处置、远程锁定或擦除；设备状态可被系统读取 | 设备不得标记生产可用；高风险接口拒绝调用 |
| 聚合支付 | 聚合支付优先，保留直连支付抽象 | 财务 + IT + 支付服务商 | 是 | 支付抽象层、支付意图、回调接收、轮询校验、退款、对账差异表 | 支付、退款、回调、轮询、对账、异常补偿全链路联调通过 | 只能使用 mock 或测试商户号，不得收真实款 |
| 酒吧人脸/年龄核验 | 第三方人脸/年龄核验接口或等效设备 | 采购 + IT + 酒吧运营 | 是 | 年龄核验接口、供应商适配器、mock 结果、人工复核状态、失败留痕 | 供应商接口或设备采购完成；生产环境能返回通过、拒绝、失败；失败可人工复核但不能口头绕过 | 酒吧入场核验不得标记通过；售酒/入场高风险流程受阻断 |
| NAS 备份 | NAS 作为 P0 备份方案 | IT/运维 | 是 | 备份任务配置、备份记录、恢复演练记录、备份失败告警 | 数据库、对象存储、配置文件按计划备份；完成至少一次恢复演练；备份权限隔离 | 不得进入真实生产；只能用于演示或预生产 |
| 账号、密钥、数据库和对象存储密码轮换 | 人工台账 + 到期提醒 + Runbook | IT/运维 | 是 | 密钥台账字段、到期提醒、轮换记录、Runbook 链接 | 生产账号、密钥、数据库密码、对象存储密码均有责任人、到期日、轮换记录和应急流程 | 不得发放长期无责任人的生产密钥 |
| 企业微信低管理成本同步 | 后台配置尽量系统化，账号绑定和同步任务自动完成 | IT + HR/运营 | 条件阻断 | 企业微信配置页、部门/员工同步任务、账号绑定、异常重试、人工补绑入口 | 企业微信应用、通讯录权限、回调地址、同步任务、失败告警配置完成 | 企业微信相关通知、质检、组织同步能力不得标记生产就绪 |
| 第三方机器人/视频/设备 API | 双向开放方案：平台开放接口 + 供应商 SDK/API 适配 | IT + 对应供应商 | 不阻断 P0 自有闭环，阻断真实联动 | 统一 API、签名、回调、字段映射、mock adapter、Webhook + 定时轮询校验 | 供应商 SDK/API、签名、回调、字段映射、错误码、对账口径确认完成 | 使用 mock adapter，不启用真实联动 |
| 160 环境 | 脚本化部署与验收 | IT/运维 | 阻断预生产/演示质量 | 部署脚本、回滚脚本、健康检查、日志检查、备份脚本 | 可重复部署、可回滚、健康检查通过、日志路径明确 | 不作为正式演示或预生产验收环境 |
| HTTPS/域名 | 当前先不绑定正式域名和 HTTPS | IT/运维 | 不阻断本地/内网开发，阻断公网生产 | 环境变量、反向代理预留、回调 URL 配置项 | 上公网或真实支付、企微、人脸回调前完成 HTTPS 和域名配置 | 仅允许内网、测试地址或 mock 回调 |

生产前置项进入实施管理时，每一项必须记录 `owner_department`、`owner_person`、`supplier_name`、`status`、`target_date`、`evidence_link`、`last_test_time`、`blocking_scope`、`fallback_mode` 和 `audit_event`。

P0 默认通过轻量系统台账/API 承载生产前置状态，不再只依赖人工台账或 Runbook 附件。接口以 `GET /api/v1/ops/production-readiness/items`、`PUT /api/v1/ops/production-readiness/items/{id}` 和 `POST /api/v1/ops/production-readiness/checks` 为准；Runbook 仍负责说明 MDM、凭证轮换和 NAS 备份恢复的实际执行步骤。

Given 系统准备从演示或预生产进入真实生产  
When 任一生产前置项状态不是“已通过”  
Then 系统不得标记为真实生产就绪  
And 必须显示阻断项、责任部门、计划完成日期和降级方式。

Given IT 更新某生产前置项为 `passed` 或 `waived`  
When 该项缺少责任人、计划日期、证据链接、阻断范围或降级方式  
Then 系统必须拒绝更新或要求补齐  
And 写入 `production_readiness.update_failed`。

Given 总控执行生产准入检查  
When 任一 MDM、支付、年龄核验、NAS、密钥轮换、企业微信、第三方联动或 160 前置项仍为 `pending`、`failed` 或 `blocked`  
Then `POST /api/v1/ops/production-readiness/checks` 必须返回阻断项清单  
And 写入 `production_readiness.checked`。
