# 完整后台管理端 UI 自动化验收矩阵 V0.1

**版本**：V0.1
**日期**：2026-06-16
**状态**：开发基线草案，可用于 Playwright 自动化补测与页面开发验收
**关联文档**：`docs/full-admin-srs-v0.1.md`、`docs/full-admin-page-permission-field-matrix-v0.1.csv`

> 重要口径：本矩阵定义完整后台应覆盖的 UI 验收，不代表当前 Web 端已全部实现。当前已实现路径仍以 `docs/ui-automation-acceptance-report-v0.1.md` 和代码验收报告为准。

## 1. 自动化验收原则

1. 每条测试必须有真实断言，不允许只判断页面打开。
2. 页面必须使用稳定选择器，优先 `data-testid`，不得依赖易变文案或 CSS 层级。
3. P0/MVP 页面至少覆盖登录、退出、权限拦截、列表查询、详情查看、创建/编辑、表单校验、空状态、错误状态。
4. 高风险操作必须覆盖二次确认、审计事件或操作结果可追踪。
5. 第三方依赖不可用时必须使用 mock/stub/route interception 覆盖成功、失败、超时、鉴权失败、限流、重复回调、异常数据。
6. 窄屏不要求全量复杂配置，但必须覆盖关键查看、审批、异常处理不遮挡、不丢字段。
7. **新页准入（全链下钻）**：新增后台页只要展示了引用型字段（商户 / 合同 / 订单 / 项目 / 空间 / 处理人 / 审计对象等），必须有一条「点击引用 → 落到目标页并定位到该行」的自动化断言才算过准入；只断言页面能打开、不验证落点，不算覆盖。断言口径见 §3.6。

## 2. P0/P1 用户路径矩阵

| 验收 ID | 对应页面 | 前置条件 | 操作步骤 | 预期结果 | 接口/数据 | 自动化建议 |
|---|---|---|---|---|---|---|
| UI-ADM-AUTH-001 | 登录/退出 | 存在后台账号 | 输入正确账号密码登录，再点击退出 | 登录后进入后台首页；退出后 token 失效并回到登录页 | `/api/auth/login` | Playwright 覆盖登录态、退出后访问受保护路由跳回登录 |
| UI-ADM-AUTH-002 | 权限拦截 | 普通员工账号无总控权限 | 直接访问 `/admin/overview` | 显示无权限或跳转到允许页面；不得泄露财务/跨主体数据 | 权限 mock/真实 RBAC | 覆盖页面级、按钮级、字段级至少一种断言 |
| UI-ADM-OV-001 | 总控驾驶舱 | 总控账号登录；有指标数据 | 打开驾驶舱，切换时间范围，点击异常入口 | 指标、异常、跳转入口正确；部分接口失败时其他卡片不消失 | overview/alerts | 断言指标文本、错误卡片、跳转路由 |
| UI-ADM-GOV-001 | 集团/综合体拓扑 | 存在集团、综合体、业态、门店 | 查看拓扑并筛选综合体 | 层级关系正确，禁用/归档节点有状态标识 | platform topology | 截图回归 + 节点状态断言 |
| UI-ADM-GOV-002 | 组织与商户档案 | 有组织/商户权限 | 新建商户、编辑、停用、搜索 | 保存成功；重复名称被拦截；停用有关联合同时提示 | organization/merchant | 使用 mock 成功和冲突两组数据 |
| UI-ADM-GOV-003 | 四类主体绑定 | 总控/财务/法务账号 | 查看唯一绑定、填写变更原因、提交绑定变更 | 运营主体、合同主体、结算主体和审计证据可见；原因为空不可提交；提交结果包含幂等键和证据附件 | subject bindings | 覆盖唯一绑定摘要、变更原因校验、变更提交、历史留痕边界 |
| UI-ADM-GOV-004 | 合同生命周期 | 存在主体绑定、空间和商户档案 | 起草合同、提交审批（指定主线最终审批人，系统逐级自动补全）、主线与平行相关部门线审批、生效、发起变更、退租结算、生成账单、归档附件 | 审批链未配置不可提交；主线与各平行相关部门线全部通过后才可生效；生效时固化收益规则并防重复出租；变更和退租结算必须有原因和幂等键；账单生成后进入应收台账；附件只显示引用 | contracts lifecycle + files | 覆盖起草、提交、主线与平行线审批、生效、变更、退租结算、周期账单、附件上传和校验错误保留表单 |
| UI-ADM-GOV-005 | 商户准入审批流水 | 已有商户档案、主体绑定、合同草稿 | 打开准入流水、核对三类准入来源、填写导出原因并导出准入审计片段 | 商户档案/主体绑定/合同摘要均可见；导出动作写入 /api/v1/evidence/events，只留痕不改业务状态 | platform/merchants + bindings + contracts + evidence/events | 空数据、缺主体、合同未审批均展示为待补齐 |
| UI-ADM-GOV-006 | 主管复核与违规阻断 | 平台管理员/主管账号 | 查看复核策略、评估合同违规、三级主管审批放行 | 违规动作默认进入 pending_review；主管级别不足被拒；审批后状态 approved/rejected 可见 | supervisor-review-policies + supervisor-review evaluate/approve | 覆盖策略列表、默认阻断、审批成功、越权/缺幂等键由后端测试覆盖 |
| UI-ADM-LEAS-001 | 招商线索 | 招商运营账号登录 | 新建线索、指派负责人、登记跟进、更新状态、查看未跟进提醒（按项目可配，出厂默认三天） | 黑名单品牌拦截新线索；线索来源必填；负责人为空不可推进；跟进记录可追踪 | leasing/leads + follow-ups | 覆盖线索录入、指派、状态流转、跟进提醒、黑名单拦截和空态 |
| UI-ADM-LEAS-002 | 招商意向 | 已有线索和目标铺位 | 线索转意向、推进五阶段、关键阶段主管确认、发起签约 | 方案报价和商务谈判阶段必须带主管确认；非待签约状态不可签约；重复推进幂等 | leasing/intents | 覆盖五阶段推进、主管确认缺失、待签约校验和重复提交 |
| UI-ADM-LEAS-003 | 品牌/客户库 | 招商运营账号登录 | 新建品牌、分类为意向/储备/合作/黑名单、拉黑或解黑 | 品牌名用于线索去重；黑名单品牌拦截同品牌新线索；拉黑解黑必须填写原因 | leasing/brands | 覆盖品牌分类、去重、黑名单拦截、拉黑原因和空态 |
| UI-ADM-LEAS-004 | 签约转商户 | 意向处于待签约 | 签约确认、自动转正式商户档案、生成四类主体默认绑定、喂入合同草稿 | 仅待签约意向可转；重复签约返回同一商户和合同草稿；四类主体默认同公司但可后续拆分 | leasing sign + platform merchants + bindings + contracts | 覆盖签约幂等、商户档案生成、主体绑定摘要和合同草稿落点 |
| UI-ADM-ACC-001 | 员工账号 | IT/HR 管理员登录 | 新增账号、填写初始密码、选择账号类型、绑定 organization_id/merchant_id、承接同意凭据/证据附件、套用权限模板 | 账号创建成功；手机号不明文落库；销售/商户账号缺 merchant_id 不可提交；跨商户创建被后端拒绝；创建后写 access.user.changed，模板应用写 permission_template.applied | access/users + permission-templates | 覆盖空账号、空密码、手机号格式、组织必填、账号类型、模板应用、跨商户拒绝 |
| UI-ADM-ACC-002 | 权限模板 | IT 管理员登录 | 复制模板、修改权限、创建临时模板、使用一次 | 原模板不变；临时模板到期或单次使用后失效 | permission templates | 覆盖到期、单次、越权编辑 |
| UI-ADM-ACC-003 | 角色装配与授权申请 | IT/HR/主管账号登录 | 配置职位角色模板、设置员工职位层级、申请临时权限、复核高危授权、查看最终权限 | 职位层级自动适配对应角色和权限模板；最终权限按角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 - 显式拒绝计算；显式拒绝优先；临时权限到期或单次使用后失效；高危授权审批通过前不生效 | access/position-role-templates + role-assignments + grant-requests + effective-permissions | 覆盖职位模板、自动适配、授权申请、主管复核、最终权限展示、显式拒绝优先和临时权限到期失效 |
| UI-ADM-TKT-001 | 票务策略 | 票务经理登录 | 配置平日/周末/节假日价，创建派生票，发布 | 基础价优先；缺节假日价不可发布；发布后列表可查 | ticket policies | 覆盖固定价/派生价互斥和发布校验 |
| UI-ADM-TKT-002 | 动态定价 | P2 功能开关开启 | 新建规则、模拟、发布、回滚 | 模拟结果可见；规则冲突提示；历史订单快照不变 | dynamic pricing | 覆盖模拟结果、冲突、回滚 |
| UI-ADM-TKT-003 | 出票与核销追踪 | 存在票务订单 | 查询订单、查看凭证、按票实例退款/补偿 | 状态流正确；重复核销幂等；退款按票实例选择（未核销票可勾选退、可退子集，已核销票禁选并提示，多人/多次票任一核销整张禁退）；**退款分级授权三档**：符合已配置自动退规则的系统自动退、无需人工，额度内员工自助退，超额度转人工审批（复核人≠发起人）；到期未核销一律作废、不产生退款单 | ticket orders/verification | 覆盖票实例选择、禁退态、批量/单张退、自动退/自助退/审批三档；「退款待确认拨付」区默认只列 `approved`，确认拨付后才记「成功」并计入已退金额 |
| UI-ADM-BAR-001 | 卡座与订台 | 酒吧主管登录 | 预订卡座、转卡、合并、释放（**无续卡**） | 人数限制、跨酒吧限制、未结账限制正确；审计链完整 | table sessions | 覆盖卡座权益不抵扣低消、跨酒吧拒绝 |
| UI-ADM-BAR-002 | 订单/出品/账单 | 存在卡座会话 | 点酒、出品、拆单、合单、结账 | 账单状态正确；低消差额补齐；合单越权拦截 | bar orders/billing | 覆盖并发结账、重复提交幂等 |
| UI-ADM-BAR-003 | 存酒 | 存在客户和库存 | 存入、领取、过期处理 | 高价值酒水必须在线校验；过期处理留痕 | stored wine/inventory | 覆盖离线禁止领取和在线失败提示 |
| UI-ADM-BAR-004 | 销售提成 | 存在卡座消费 | 查看销售/非销售提成、调整、结算 | 销售卡座 15%，非销售 10%，不含通票；调整留痕 | commissions | 覆盖提成边界和财务审批 |
| UI-ADM-BAR-005 | 商品管理 | 平台管理员登录；有商品目录 | 查看商品列表、录入售价与内部结算价 | 列表展示售价与内部结算价（未录入显示占位 —）；录入按钮受 bar.product.manage 门控；售价/内部价非负校验 | GET/POST /api/v1/bar/products | 覆盖列表渲染、内部价占位、录入权限按钮门控、非负校验 |
| UI-ADM-AMU-001 | 项目与场次 | 游乐主管登录 | 创建项目、排场次、限流、关闭 | 容量限制正确；安全告知缺失不可开放 | amusement sessions | 覆盖容量边界和安全告知校验 |
| UI-ADM-AMU-002 | 团体/包场 | 游乐主管登录；有团体预约 | 查看团体/包场预约与容量占用 | 团体占用计入容量；跨项目越权预约被拒 | amusement group-bookings（`amusement.group_booking.view`） | 覆盖容量占用、空态、跨项目越权拒绝 |
| UI-ADM-AMU-003 | 安全巡检/事故 | 存在安全事件 | 上报、上传附件、处理、关闭 | 高等级事故需审批；缺处理记录不可关闭；高/严重事故与硬超容自动停运（open→paused）| incidents/files | 覆盖附件失败、重新打开、自动停运联动 |
| UI-ADM-INV-001 | 库存台账 | 仓库账号登录 | 入库、出库、盘点、调整 | 不允许负库存；差异处理需原因；高价值在线核销 | inventory ledger | 覆盖负库存和盘点差异 |
| UI-ADM-FIN-001 | 应收应付台账 | 财务账号登录 | 生成应收、部分核销、调整、导出 | 原金额、已核销、余额正确；调整和导出留痕 | finance ledger | 覆盖部分核销、逾期、导出审计 |
| UI-ADM-FIN-002 | 对账差异 | 存在第三方账单 | 导入对账、匹配、处理差异、填写关闭原因并正式关闭 | 差异原因必填；正式关闭必须有 `close_reason`、幂等键和证据附件引用；关闭后不可无痕改写；重复关闭返回同一处理结果 | finance/reconciliation batches + differences + close | 覆盖金额不一致、重复账单、关闭原因缺失、附件引用、幂等关闭和关闭后只读 |
| UI-ADM-MER-001 | 商户服务工单 | 商户运营登录 | 创建维修/投诉/整改，派单，关闭 | 逾期提醒；整改关闭需证据 | merchant service work orders | 覆盖附件缺失和超时提醒 |
| UI-ADM-MER-002 | 商户授权工作台 | merchant_operator 账号 | 查看授权工单/账单、确认授权范围、尝试进入平台财务页 | 授权范围内数据可见；商户角色只显示自身 merchant_id；平台财务页显示权限不足；确认动作产生留痕结果 | merchant-service/work-orders + merchant-billing/bills | 覆盖授权工单/账单分表、空状态、越权拦截、确认留痕；后端数据范围策略是安全边界 |
| UI-ADM-MER-003 | 商户专区·我的账单 | 商户形态账号（`accountForms: ["merchant"]`）登录，权限 `merchant.billing.view` | 打开 `/admin/merchant-zone/bills`，查看账单汇总与明细 | 请求头 `x-terminal-type = merchant_portal`；汇总区出现「账单总数」；明细表出现本商户账单号与金额（现有断言：`MBILL-1` / `120,000.00`） | `merchant-zone-bills-page` / `merchant-zone-bills-summary` / `merchant-zone-bills-table` | 只看本商户；平台形态账号在该路由不可见（账号形态裁决：商户专区内容验收已从平台会话移到商户会话） |
| UI-ADM-MER-004 | 商户专区·我的工单 | 同上，权限 `merchant.service_work_order.view` | 打开 `/admin/merchant-zone/work-orders`，查看工单并自助提单 | 汇总区出现「工单总数」；明细表出现本商户工单（现有断言：`空调漏水`）；自助提单表单可见 | `merchant-zone-work-orders-summary` / `merchant-zone-work-orders-table` / `merchant-zone-work-order-form` | 商户侧自助提单入口，与平台侧 UI-ADM-MER-001 是同一工单域的两端 |
| UI-ADM-MER-005 | 商户专区·我的对账 | 同上，权限 `merchant.portal.settlement.view` | 打开 `/admin/merchant-zone/settlements`，查看结算单并发起异议 | 汇总区出现「结算单」；明细表出现本商户结算单（现有断言：`cst-1`）；异议表单可见 | `merchant-zone-settlements-summary` / `merchant-zone-settlements-table` / `merchant-zone-settlement-dispute-form` | 异议与客人投诉并入同一通道（见 BSM「商户提出的投诉与异议，与客人投诉并入同一通道」条） |
| UI-ADM-MER-006 | 商户专区·我的合同 | 同上，权限 `contract.view` | 打开 `/admin/merchant-zone/contracts`，查看本商户合同 | 汇总区出现「合同总数」；明细表出现本商户合同（现有断言：`ntract-1`） | `merchant-zone-contracts-summary` / `merchant-zone-contracts-table` | 只读；合同变更走平台侧审批链，商户专区不提供改约入口 |
| UI-ADM-MER-007 | 商户专区·费用通知 | 同上，权限 `merchant.portal.fee_recipient.manage` | 打开 `/admin/merchant-zone/fee-recipients`，维护费用通知接收人 | 汇总区出现「接收人配置」；明细表出现已配号码（现有断言：`15577000001`）；接收人表单可见 | `merchant-zone-fee-recipients-summary` / `merchant-zone-fee-recipients-table` / `merchant-zone-fee-recipient-form` | 该页是「重要事项短信通知·给商户」的收件人来源；该页现按明文显示接收人手机号（e2e 断言可见完整号码）。**2026-08-07 用户已裁：不脱敏**（DEC-R8-40，援引 DEC-R7-33 先例）——脱敏项为封闭清单、没有第八项，「费用通知接收人手机号」不属清单内项，该页明文显示即合规，展示层无须改造、e2e 断言完整号码保持不变；findings R9-B-01 已 `resolved` |
| UI-ADM-MER-008 | 商户专区·我的分红 | 同上，权限 `finance.dividend.view`，主理人身份 | 打开 `/admin/merchant-zone/dividends`，查看各期分红 | 分红表出现期次、金额与状态（现有断言：`2026-Q2` / `1,040.00` / `已审批`）；**页面无审批入口**（只读视图，V299） | `merchant-zone-dividends-page` / `merchant-dividend-table` | 主理人对本商户经营数据完全可见；发放与审批一律在平台侧 UI-ADM-FIN-007 完成 |
| UI-ADM-FAC-001 | 楼宇设施设备/工单/巡检 | 物业/工程账号登录、存在空间点位 | 建设备台账、维保到期处理、设备告警转工单、派单处理验收、巡检排程、异常转整改、主管确认 | 设备告警/故障自动生成工单；维保到期提醒；整改类异常必须转工单且主管确认闭环；巡检超期告警；能耗抄表不在此页（归能耗域） | facility equipment/work-orders/inspections | 覆盖设备台账、维保到期、告警转单、工单全流转、巡检排程与异常闭环（前端「楼宇运营工作台」/admin/facility/workbench 已在 adminRoutes，后端 V165-167 已落地；受闸页面矩阵已对齐 adminRoutes，仅 Playwright e2e 待补）|
| UI-ADM-ENE-001 | 能耗表计/抄表/能源费 | 能源/财务账号登录、存在表计 | 维护表计、抄表录入、抄表审核、异常复核、总分表核对、商户能源费结算 | 异常读数（突增/突降/零/倒走）转人工复核；总分表抹差；**公区能耗只归集做成本分析，不向商户分摊、不生成公区能源费账单**（公区公摊统一含在物业费中；原「公区分摊四模式可配」作废，依据 ENUM「公区能源费」条、DEC-R7-35）；商户能源费按独立表计实际用量与实际费用代收、不得加价，结算写应收台账 | energy meters/readings/settlements | 覆盖表计台账、抄表审核、异常复核、差值核对和能源费结算（前端「能源工作台」/admin/energy/workbench 已在 adminRoutes，后端 V168 已落地；受闸页面矩阵已对齐 adminRoutes，仅 Playwright e2e 待补）|
| UI-ADM-INT-001 | 供应商适配器 | IT/集成管理员登录 | 配置 endpoint、字段映射、签名、凭证引用 | 密钥不明文展示；字段缺失不可启用 | integration adapters | 覆盖鉴权失败、密钥引用脱敏 |
| UI-ADM-INT-002 | 回调/轮询/补偿 | 存在第三方事件 | 查看四类回流、登记轮询校验、立即重试、处理补偿 | 重复请求幂等；签名失败拒绝；轮询补偿来源明确；不得伪造第三方生产补偿执行 | integration events | 覆盖成功、失败、超时、限流、重复回调、内部补偿处理 |
| UI-ADM-INT-003 | 第三方开关 | 平台管理员登录；存在能力开关 | 查看全部第三方/集成开关及接入状态、切换某开关 | 默认全部关闭；每开关标注关闭口径/开启要求/当前 API 或 SDK 接入状态；仅平台管理员可切换，非平台账号 fail-closed 拒绝；切换写 terminal_switch.changed 留痕 | terminals/capability-switches（GET 列表 / PUT 切换） | 覆盖列表渲染、接入状态标注、平台切换、非平台越权拒绝、空/错状态 |
| UI-ADM-EVD-001 | 审计事件 | 审计账号登录 | 搜索、筛选、查看详情、导出并留痕 | 审计事件不可编辑；导出通过 evidence event 自身留痕；无匹配和读取失败可见 | audit events | 覆盖 trace_id 查询、前后值详情、导出 EventBatchRequest 和错误状态 |
| UI-ADM-ANA-001 | 经营分析 | 总控/运营/财务账号登录；存在指标快照、定义和异常数据 | 查看指标快照、指标定义和异常，按业务域筛选，锁定复盘快照 | 三类数据均可见；筛选后只显示对应业务域；空数据显示“暂无经营指标”；锁定复盘只产生本地审计结果，不伪造 AI/BI 生产结论 | analytics/business-metric-snapshots + metric-definitions + metric-anomalies | 覆盖三接口聚合、业务域筛选、空状态、错误状态、窄屏和复盘锁定留痕 |
| UI-ADM-ANA-002 | 集团总览 | 平台总控账号登录（无 merchant scope） | 查看多综合体集团级经营汇总 | 平台级跨综合体汇总可见；merchant-scoped 账号被 fail-closed 拒绝（PLATFORM_SCOPE_REQUIRED） | analytics/group-overview（`analytics.group.view`） | 覆盖平台汇总、商户账号越权拒绝、空状态 |
| UI-ADM-RISK-001 | 风控预警 | 平台风控账号登录；有风险告警 | 查看高风险客户/员工/第三方/生产预警并处理 | 预警可见；处理写留痕；merchant-scoped 账号 fail-closed 拒绝（平台级分析） | analytics/risk-alerts（`analytics.risk_alert.view`/`analytics.risk_alert.manage`） | 覆盖预警列表、处理留痕、越权拒绝、空/错状态 |
| UI-ADM-OPS-001 | 生产证据 | IT 管理员登录 | 登记 NAS/密钥/备份/恢复证据，更新状态 | 只保存 credential_ref/evidence_ref，不保存真实密钥；到期提醒可见 | ops readiness | 覆盖证据缺失、过期、已验证状态 |
| UI-ADM-VIS-001 | 完整后台视觉回归 | 平台管理员登录；Mock API 有基础数据 | 桌面打开总控驾驶舱和生产 blocker 与证据页；移动端打开酒吧订单和动态定价页 | 截图基线稳定；无材料级横向溢出；导航、表格和主按钮不遮挡 | full-admin-visual | `tests/e2e/full-admin-visual.spec.ts` 覆盖 4 张截图基线 |

## 2A. 客人端与一体机验收范围（权威指向）

> 本矩阵 §2 为后台管理端（`UI-ADM-*`）路径。客人端与一体机的端到端验收口径在**专属文档**，本节统一指向，避免误判"客人端/一体机无验收"：

| 范围 | 验收权威文档 | 覆盖 |
|---|---|---|
| 客人端（App/小程序） | `customer-app-miniapp-development-design-v0.1.md` §8 验收标准、**§9A 两端一致性/订台定金异常/第三方渠道查询 E2E（G/W/T）**；`v099-v124-feature-implementation-and-acceptance-spec-v0.1.md` | 购票/订台定金/我的订单票权益/积分摘要/验票二维码；储值/付费订阅/签到生日/订卡邀请函见 `member-loyalty-and-stored-value-development-design-v0.1.md` + v099-v124 |
| 触摸屏一体机（kiosk） | `terminal-page-product-prd-v0.1.md` §4 终端能力矩阵 + 本节一体机专项 G/W/T | 售票型授权服务人员辅助购票/取票；非售票型权益领取/订台/吧台买酒/游乐状态/检修记录 |

**一体机（kiosk）专项 Given/When/Then（补统一口径）：**
- Given 非售票型一体机扫客人码，When 展示可领权益，Then 只展示该客人当前可领的赠饮/礼物/积分兑换并回写领取状态，不暴露他人数据。
- Given 一体机订台/吧台买酒，When 现金或 POS 支付，Then 必须由授权员工记录到订单/账单/日结/对账（一体机不脱离财务闭环）。
- Given 售票型一体机，When 未授权工作人员尝试售票，Then 拒绝并留痕。
- Given 绑定某酒吧门口/前台的售票型一体机，When 授权服务人员辅助购票，Then 只展示并售卖该酒吧单店门票，不展示综合体通票。
- Given 一体机断网/开关拉取失败，When 资金/核销/库存类写操作，Then 后端 fail-closed 拒绝并留痕（见 operation-traceability §5.6 一体机协调）。

## 3. 通用 Given/When/Then 验收

### 3.1 表单校验

- Given 用户打开任一新增/编辑表单，When 必填字段为空提交，Then 系统在对应字段显示错误，保存按钮不产生成功提示，已填字段不被清空。
- Given 用户输入超长文本或非法格式，When 失焦或提交，Then 系统显示明确错误，并阻止请求或阻止保存。

### 3.2 权限边界

- Given 用户没有页面访问权限，When 直接输入 URL，Then 系统拦截访问，不渲染页面敏感数据。
- Given 用户有页面读权限但无写权限，When 打开页面，Then 新增、编辑、删除、审批按钮不可用或不可见，后端同样拒绝写请求。

### 3.3 错误与恢复

- Given 业务接口返回 500，When 页面请求失败，Then 页面显示可读错误、保留筛选条件，并提供重试动作。
- Given 第三方接口超时，When 用户触发同步或补偿，Then 页面显示处理中/失败状态，不允许重复产生业务结果。

### 3.4 并发与幂等

- Given 两个用户同时编辑同一对象，When 后提交的用户保存，Then 系统提示数据已变化并要求刷新或重新确认。
- Given 用户连续点击提交按钮，When 请求重复发送，Then 系统只产生一次业务结果和一次有效审计事件。

### 3.5 移动端/窄屏

- Given 视口宽度为 390px，When 用户打开关键查看、审批或异常处理页面，Then 核心字段、状态、主按钮不遮挡，横向表格提供可用的折叠或滚动方式。

### 3.6 全链下钻定位（新页准入项）

> 2026-07-28 补：本条是**新页准入门槛**，不是可选加分项。此前矩阵只约束页面自身的查询 / 详情 / 增删改，新页不做下钻也能过准入，引用字段就此变成看得见、点不动的数据孤岛——对账时看到一个商户号却跳不过去查它，只能另开一页手工搜。

- Given 任一后台页展示了引用型字段（商户 / 合同 / 订单 / 项目 / 空间 / 处理人 / 审计对象等），When 用户点击该引用，Then 路由跳到目标业务页并带 `?focus=<对象ID>`，目标页命中行加高亮类 `admin-row-selected`，同表其他行不带该类。
- Given 引用指向的对象已归档、已删除或当前账号无权查看，When 用户点击该引用，Then 目标页给出可读的「未找到 / 无权限」提示，不白屏、不静默停在原页。
- Given 该引用字段是表单输入框或组件内部 state（非展示型引用），When 评估准入，Then **不要求**做成下钻——本条只约束展示给业务人员看的引用值。

**统一实现与断言口径**：展示侧一律用 `<TraceLink>` + `navigateTo`；落地侧一律用 `focusObjectId` + `admin-row-selected`（2026-07-28 起为唯一行高亮类，原并存的 `admin-row-focus` 已删除）。现成样板断言见 `apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx`、`TraceLinkP2A.test.tsx`、`TraceLinkP2c.test.tsx`、`HandlerTrace.test.tsx`、`ContractWorkbenchPage.test.tsx`——新页照抄其中一条即可，不需另建测试框架。

## 4. 截图与视觉回归基线

已建立完整后台首批截图基线：总控驾驶舱桌面、生产 blocker 与证据桌面、酒吧订单/出品/账单移动端、动态定价移动端。后续建议继续补权限模板复杂权限树、票务策略价格矩阵、卡座订台转卡/合并弹窗（无续卡）、应收应付台账、供应商适配器密钥脱敏与回调失败详情。

## 5. 未覆盖不得上线条件

以下任一项未覆盖，不得声称完整后台可上线：权限拦截只做前端隐藏，没有服务端拒绝证据；财务、库存、核销、退款、补偿、权限变更没有审计事件；第三方接口没有 mock/stub 覆盖失败和重复请求；动态定价、票务策略、卡座权益、低消、提成之间缺少交叉边界测试；移动端关键查看/审批/异常处理存在遮挡或核心字段不可见。**新增后台页展示了引用型字段，却没有「点击引用 → 落目标页并定位」的自动化断言（见 §3.6）**。

> A2 2026-07-28：移除续卡现行能力表述，对齐 DEC-SEAT-NO-RENEW-001 / bar PRD §7.3A。
