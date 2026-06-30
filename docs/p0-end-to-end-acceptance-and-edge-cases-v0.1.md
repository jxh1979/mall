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

### AC-TKT-02A 售票型一体机授权服务人员辅助购票

Given 售票型一体机已绑定点位、设备编号、`kiosk_mode=ticketing` 且票务模块开关打开  
When 授权服务人员为到店客人选择票种并完成支付引导  
Then 系统必须按平日/周末/节假日基础价及其派生价校验价格  
And 必须先按一体机绑定点位过滤可售票种，再锁定库存、创建订单和支付二维码  
And 支付成功后生成票实例、权益实例和防伪二维码  
And 写入 `kiosk.ticket.order_created`、`payment.gateway_notified`、`ticket.issued`  
And 非售票型一体机不得出现购票入口
And 绑定某酒吧门口或前台的售票一体机只能售卖该酒吧单店门票，不得售卖综合体通票。

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

## 8B. 端到端验收补齐（R6 全闭环(2026-06-19)）

> 背景（依据 R6 审计）：以下端到端闭环缺 Given/When/Then 与边界场景——现金支付、POS 机支付、按杯损耗（缺 `actual_pour_volume`）、班结对账（开班/班结/现金差异/超阈值复核）、存酒核身字段被拒与取酒标签+昵称比对、跨商户/跨项目越权被拒留痕、退菜退酒/反结、游乐超容落事件。本节按既有 AC 风格补齐，编号延续 AC-FIN/INV/AUTH/AMU/BAR 各域。

### AC-FIN-04 现金支付

Given 酒吧账单或票务订单选择现金支付  
When 员工收取现金并在系统登记实收金额与找零  
Then 系统必须记录现金收款（实收/应收/找零，金额=分 D1）、操作员工、终端和时间  
And 写入现金收款事件并计入当班现金台账（与 AC-FIN-06 班结现金核对联动）  
And 现金收款不得绕过日结锁定（AC-FIN-03）；日结后更正只能走补录/冲正/调整单。

### AC-FIN-05 POS 机支付

Given P0 选择聚合支付，POS 机作为受理终端  
When 客人在 POS 机完成扫码/刷卡支付  
Then 系统通过支付抽象层（AC-FIN-01）受理，记录 POS 终端编号、交易流水号、支付通道和金额  
And 支付成功后才推进业务单据（出票/结账/开台抵扣）  
And POS 回调按交易流水号幂等（重复回调只认一笔）；回调成功但轮询不一致时标记差异进对账补偿（对齐 AC-OPEN-03）。

### AC-FIN-06 班结对账（开班 / 班结 / 现金差异 / 超阈值复核）

Given 员工开班并登记备用金（开班现金底数，金额=分 D1）  
When 当班结束发起班结  
Then 系统必须汇总当班应收（现金/聚合支付/POS 分通道）、实点现金，计算现金差异 = 实点现金 −（开班备用金 + 当班现金应收 − 现金退款/支出）  
And 写入开班事件与班结事件，绑定员工、终端、班次时间  
And 现金差异在阈值内允许班结通过并留痕  
And 现金差异超阈值时必须转主管复核审批后方可班结，不得自动通过  
And 班结数据进入日结对账（AC-FIN-03），班结锁定后更正只能走冲正/调整单。

### 边界用例（支付与班结补充）

| 场景 | 预期 |
|---|---|
| 现金支付登记金额与实收不符 | 以系统登记为准并计入现金差异，超阈值转主管复核 |
| POS 回调重复 | 按交易流水号幂等，只认一笔有效结果 |
| 班结时现金差异超阈值 | 阻断自动班结，强制转主管复核审批并留痕 |
| 未开班直接收现金 | 拒绝或标记异常，当班现金台账须先有开班备用金记录 |
| 班结后发现漏记现金收款 | 生成补录/调整单，不覆盖原班结记录 |

### AC-FIN-07 对账差异正式关闭

### AC-FAC-01 楼宇设备告警自动转工单与巡检闭环
- Given 已建楼宇机电设备台账（电梯/暖通/配电/给排水）并配置维保周期；
- When 设备上报告警/故障，或巡检任务发现重类异常；
- Then 自动生成物业工单并派单（pending_assign→assigned→processing→accepted/closed）；巡检异常走 rectifying→rechecked→主管确认 closed；维保到期生成维保单。
- 边界：轻类异常仅记录、重类强制转工单；超期工单 mark-overdue 升级；能耗抄表不在巡检内重复抄（归能耗域）。

### AC-ENE-01 能耗抄表异常复核与能源费结算
- Given 已建表计台账（电/水/燃气/冷热量）按公区/商户/回路归集；
- When 抄表录入读数；
- Then 正常读数 recorded→reviewed；异常（突增/突降/零读数/倒走）转 abnormal 进人工复核；公区能源费按四模式分摊、结算写 ar_ap_items(source_type=energy_fee, direction=receivable)。
- 边界：总分表抹差核对；远程表自动读为主、异常才转人工。

Given 对账差异处于 open、matched、resolved 或 ignored 状态
When 财务人员提交关闭请求并填写 `idempotency_key`、`close_reason` 和证据附件引用
Then 系统必须将差异正式关闭并写入关闭原因、操作人、时间和审计事件
And 缺少关闭原因必须拒绝；重复提交同一幂等键必须返回同一关闭结果；关闭后不得无痕改写。

### AC-INV-04 按杯损耗（actual_pour_volume）

Given 高价值酒水按杯/按盎司出品，配置标准出品量（standard_pour_volume）  
When 出品并登记实际出品量 `actual_pour_volume`  
Then 系统必须记录实际出品量并按 `actual_pour_volume` 扣减对应库存（在线扣减，不得离线，对齐 AC-INV-01）  
And 损耗 = 实际扣减量 − 应出品量（按杯数 × 标准出品量），损耗写入留痕  
And 缺失 `actual_pour_volume` 必填字段时拒绝出品登记，不得用标准量静默替代真实出品。

### AC-INV-05 存酒登记核身字段被拒 / 取酒标签 + 昵称比对

Given 客人要求存酒  
When 吧台登记但缺核身必填字段（瓶底标签数值、照片、手机号/昵称、登记员工之一缺失）  
Then 系统必须拒绝存酒登记并提示缺失字段，不得生成存酒记录  
And `customer_id` 可为空（散客存酒按手机号/昵称承接），但瓶底标签、手机号/昵称等核身字段不得为空  
When 客人取酒  
Then 系统必须比对瓶底标签数值与登记姓名/昵称一致后方可放行取酒，并记录领取签收与状态变更  
And 标签数值或姓名/昵称不匹配时拒绝取酒并留痕（对齐 AC-INV-02）。

### 边界用例（存酒与按杯损耗补充）

| 场景 | 预期 |
|---|---|
| 取酒时瓶底标签数值不匹配 | 拒绝取酒并提示标签不符，转人工核实 |
| 取酒时姓名/昵称不匹配 | 拒绝取酒并留痕，不得仅凭标签放行 |
| 按杯出品未登记 actual_pour_volume | 拒绝出品登记，必填项不得缺失 |
| 实际出品量异常偏大（疑似多倒/漏登） | 标记损耗异常进盘点/报损复核（AC-INV-03） |

### AC-AUTH-04 跨商户 / 跨项目越权被拒留痕

Given 员工 A 归属酒吧商户 M1（或游乐项目 P1）  
When 员工 A 尝试核销/操作归属其他商户 M2 或其他项目 P2 的权益/单据  
Then 系统必须按商户/项目归属与授权关系拒绝该操作（对齐票务 §5.4 跨商户隔离、§5.3 第 7 步商户校验）  
And 拒绝必须写入越权事件并留痕（操作员工、目标商户/项目、被拒权益/单据、终端、时间）  
And 越权拒绝进入事件中心供风控订阅（票务 §10.8"跨商户越权核销"）。

### AC-AUTH-05 最终权限计算与显式拒绝优先

Given 员工同时拥有角色权限、基础模板、有效临时模板和员工单独授予权限
When 后台计算该员工最终权限
Then 最终权限必须按“角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝”得到
And 显式拒绝优先级最高；被拒绝的权限不得因角色或临时授权再次生效。

### AC-AUTH-06 角色装配与高危授权审批

Given 平台管理员配置职位角色模板，HR 设置员工职位和层级
When 员工申请临时权限或高危授权
Then 系统必须按职位层级自动适配默认权限，并将需审批的授权进入主管复核流程
And 审批通过前高危权限不得生效；临时权限到期或单次使用后必须失效并留痕。

### 边界用例（越权补充）

| 场景 | 预期 |
|---|---|
| 酒吧员工核销游乐权益 | 拒绝并留痕，仅可核销本商户/项目授权范围权益 |
| 跨项目主体查看他人结算 | 拒绝，商户后台只能看自己的核销与结算（票务 §13） |
| 临时授权过期后跨域操作 | 拒绝并写到期事件（对齐 AC-AUTH-02） |

### AC-BAR-06 退菜退酒 / 反结

Given 酒吧账单已下单（含已出品酒水）或已结账  
When 发起退菜/退酒（未结账）或反结（已结账撤销）  
Then 退菜/退酒必须记录退品项、数量、原因、操作员工与审批（按金额/权限阈值）  
And 已出品酒水退酒须联动库存与损耗口径（对齐 AC-INV-04），不得静默改库存  
And 反结必须撤销原结账并生成冲正，不得直接删除原账单/支付记录  
And 反结后重新结账须重新计算金额，原提成/赠酒成本等按冲正联动（对齐 AC-BAR-05）  
And 日结锁定后（AC-FIN-03）退菜退酒/反结只能走补录/冲正/调整单。

### 边界用例（退菜退酒 / 反结补充）

| 场景 | 预期 |
|---|---|
| 已结账后反结 | 生成冲正，原账单/支付不得物理删除 |
| 退已出品高价值酒 | 联动库存回补/损耗复核，不得静默改库存 |
| 反结涉及已计提成 | 提成生成冲正记录，不覆盖原提成（AC-BAR-05 边界一致） |
| 日结后发起反结 | 拒绝直接修改，只能走补录/冲正/调整单 |

### AC-AMU-06 游乐超容落事件

Given 某游乐项目场次容量已满或核销请求超过剩余容量  
When 游客继续核销权益  
Then 系统必须按容量不足拒绝核销（对齐 AC-AMU-05 安全与容量同时校验）  
And 超容拒绝必须落事件中心（写入超容/容量不足事件，绑定项目、场次、时段、被拒权益、终端、时间）  
And 安全巡检通过不得绕过容量控制（AC-AMU-05），容量校验与超容留痕均不可缺失。

### 边界用例（游乐超容补充）

| 场景 | 预期 |
|---|---|
| 容量已满仍尝试核销 | 拒绝并落超容事件，可引导下一场次 |
| 并发核销争抢最后名额 | 以服务端锁定/版本号结果为准，只允许容量内一笔成功，其余落超容事件 |
| 人工放行绕过容量 | 不允许；人工放行（AC-AMU-02）须独立审批留痕，不得规避容量与超容留痕 |

### AC-LEAS-01 招商线索到签约转商户

Given 招商线索已录入并指派负责人
When 线索转意向、推进到待签约并完成签约确认
Then 系统必须生成正式商户档案、四类主体默认绑定和合同草稿
And 关键阶段推进必须有主管确认；黑名单品牌不得转为新线索；重复签约必须幂等返回同一商户和合同草稿。

### AC-CON-01 合同起草到生效

Given 合同草稿已填写商户、空间、合同主体、结算主体、租金和收益规则
When 提交审批并完成业务、财务、法务并行会签和终审
Then 合同才能生效，生效时必须固化收益规则快照并绑定铺位
And 审批链未配置不可提交；并行会签未全过不可生效；同一铺位重复出租必须拒绝。

### AC-CON-02 合同变更、退租结算与账单

Given 合同已生效
When 发起合同变更、退租结算或周期账单生成
Then 变更必须走同一审批链，退租结算必须按待结算、保证金抵扣中、欠费追缴中、已结清或有争议留状态
And 租金、物业费和能源费账单必须进入应收应付台账；争议结算须主管裁决后继续。

## 8A. 高风险 operation 验收映射

本节用于把端到端验收标准和 OpenAPI operationId 直接绑定，避免开发、测试或供应商只看到业务验收描述但无法定位接口。P0 不要求 181 个 operation 全量逐条写 Given/When/Then；但以下高风险闭环必须能按 AC 编号、路径和 operationId 追溯。

| 验收项 | 高风险闭环 | 必须覆盖的 OpenAPI 路径 / operationId | 验收底线 |
|---|---|---|---|
| AC-TKT-02 | 动态二维码核销 | `POST /api/v1/ticketing/verifications/preview` / `previewTicketVerification`；`POST /api/v1/ticketing/verifications` / `createTicketVerification` | 预览和正式核销均校验票、权益、签名、有效期、终端和幂等；失败必须有 `verification.failed` |
| AC-TKT-02A | 售票型一体机辅助购票 | `GET /api/v1/terminals/kiosks/{device_id}/config` / `getKioskConfig`；`POST /api/v1/ticketing/orders` / `createTicketOrder`；`POST /api/v1/ticketing/orders/{order_id}/pay-intents` / `createTicketOrderPayIntent`；`POST /api/v1/ticketing/orders/{order_id}/issue` / `issueTicket` | 只有 `kiosk_mode=ticketing` 且授权服务人员登录后可售票；酒吧门口/前台只售卖本酒吧单店门票；支付成功后才出票和发权益 |
| AC-TKT-03 | 退款与权益冲正 | `POST /api/v1/ticketing/refunds` / `requestTicketRefund`；`POST /api/v1/ticketing/refunds/{refund_id}/approve` / `approveTicketRefund`；`POST /api/v1/finance/refunds` / `createFinanceRefund` | 已核销权益不得删除，只能退款、撤销、冲正并留痕 |
| AC-BAR-02 | 卡座免通票入场名额 | `POST /api/v1/bar/reservations/{id}/check-in-guest` / `checkInBarReservationGuest` | 只能进入预定卡座所在酒吧；不得超过人数额度；不得抵扣最低消费 |
| AC-BAR-04 | 酒吧年龄/人脸核验 | `POST /api/v1/bar/age-verifications` / `createBarAgeVerification` | 第三方或等效设备未通过时不得生产放行；人工复核只能走异常留痕 |
| AC-BAR-05 | 卡座销售提成 | `POST /api/v1/bar/bills/{bill_id}/commissions/calculate` / `calculateBarTableCommission` | 销售人员 15%、非销售人员 10%；提成基数排除通票、权益、赠酒、退款、免单和不可计提调整 |
| AC-INV-01 | 高价值酒水库存扣减 | `POST /api/v1/inventory/stock-movements` / `createStockMovement`；`POST /api/v1/inventory/stock-movements/{movement_type}` / `createTypedStockMovement` | 高价值酒水不得离线扣减；失败必须阻断日结或进入补偿 |
| AC-INV-02 | 存酒与取酒 | `POST /api/v1/inventory/saved-bottles` / `createSavedBottle`；`POST /api/v1/inventory/saved-bottles/{id}/claim` / `claimSavedBottle` | 存酒、取酒、作废均必须有客户、员工、证据和状态变更 |
| AC-FIN-01 | 聚合支付与直连抽象 | `POST /api/v1/finance/payment-intents` / `createPaymentIntent`；`POST /api/v1/bar/bills/{bill_id}/pay-intents` / `createBarBillPayIntent`；`POST /api/v1/ticketing/orders/{order_id}/pay-intents` / `createTicketOrderPayIntent` | P0 默认聚合支付；直连只保留抽象，不默认启用 |
| AC-FIN-02 | 完整应收应付台账 | `GET /api/v1/finance/ar-ap-items` / `listArApItems`；`POST /api/v1/finance/ar-ap-items` / `createArApItem`；`POST /api/v1/finance/ar-ap-items/{id}/payments` / `createArApPayment` | 台账必须绑定来源单据、四类主体和收付款记录 |
| AC-FIN-07 | 对账差异正式关闭 | `POST /api/v1/finance/reconciliation/differences/{difference_id}/close` / `closeFinanceReconciliationDifference` | 关闭必须有幂等键、关闭原因和证据附件引用；关闭后不得无痕改写 |
| AC-AUTH-05 | 最终权限计算与显式拒绝 | `GET /api/v1/access/users/{id}/effective-permissions` / `getUserEffectivePermissions`；`GET /api/v1/access/effective-permissions` / `getEffectivePermissions` | 显式拒绝优先于角色、模板、临时和单独授予权限 |
| AC-AUTH-06 | 角色装配与高危授权审批 | `GET/POST /api/v1/access/position-role-templates` / `listPositionRoleTemplates`、`createPositionRoleTemplate`；`POST /api/v1/access/grant-requests` / `createAccessGrantRequest`；`POST /api/v1/access/grant-requests/{id}/review` / `reviewAccessGrantRequest` | 职位层级自动适配；高危授权审批通过前不生效；临时权限到期失效 |
| AC-LEAS-01 | 招商线索到签约转商户 | `POST /api/v1/leasing/leads` / `createLeasingLead`；`POST /api/v1/leasing/intents` / `createLeasingIntent`；`POST /api/v1/leasing/intents/{intent_id}/advance` / `advanceLeasingIntent`；`POST /api/v1/leasing/intents/{intent_id}/sign` / `signLeasingIntent` | 签约后生成正式商户、四类主体默认绑定和合同草稿；重复签约幂等 |
| AC-CON-01 | 合同起草到生效 | `POST /api/v1/contracts` / `createContractDraft`；`POST /api/v1/contracts/{contract_id}/submit` / `submitContract`；`POST /api/v1/contracts/{contract_id}/approve` / `approveContractStage`；`POST /api/v1/contracts/{contract_id}/activate` / `activateContract` | 会签全过后才可生效；生效固化收益规则并绑定铺位 |
| AC-CON-02 | 合同变更、退租结算与账单 | `POST /api/v1/contracts/{contract_id}/changes` / `raiseContractChange`；`POST /api/v1/contracts/changes/{change_id}/approve` / `approveContractChange`；`POST /api/v1/contracts/{contract_id}/settle` / `settleContract`；`POST /api/v1/contracts/settlements/{settlement_id}/advance` / `advanceContractSettlement`；`POST /api/v1/contracts/{contract_id}/period-bills` / `generateContractPeriodBills` | 变更走审批链；退租结算保留状态；账单进入应收应付台账 |
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
