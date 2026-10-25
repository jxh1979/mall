# 楼宇运营业务域 PRD（V1.11·据需求重建）

> **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。

> 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。

> 生成：2026-06-27。依据：M1 需求采集 §3-§6 + 业务 HTML §十（地产运营：设备/物业工单/能耗/巡检枚举）+ `platform-governance-map-for-operations-v0.1.html`「楼宇运营」+ `business-system-map-for-operations-v0.1.html`「物业服务/楼宇运营」+ V165-V168 迁移 + `com.complex.platform.facility` / `com.complex.platform.energy` Controller 与 Service。本文向业务 HTML §十对齐；冲突以业务 HTML 为最终解释权，本文同时逐字登记本轮真实 DB CHECK 与实际端点。
> 重建动因：据需求重建，旧骨架枚举漂移(设备3→6态/工单类型3→6/表计3→4/巡检4技术态→7业务态)已纠正。楼宇本体运营不能继续混在员工工作设备、游乐设备或旧商户服务工单里；V165-V168 已把楼宇机电设备、维保/物业工单、楼宇巡检、能耗抄表拆成独立领域骨架，尚不等于权威业务闭环全部完成，本文补齐产品/技术基线，避免后续再用旧 V022 工单或游乐安全巡检替代楼宇域。

## 0. 定位与边界
- 楼宇运营 = 综合体建筑本身的机电设备台账、维保/物业工单、楼宇巡检、能耗抄表与能源费应收。
- 数据隔离口径 = 综合体/项目级。表上保留 `project_id`，多数写路径在 Service 层调用 `authorizationService.requirePlatformScope()`；设施工单与商户服务工单彻底分表，商户侧自助提单应走 V022 `merchant_service_work_orders`，当前 V166 仍允许 `source=merchant_self` 的实现缺口见 §3/§6 C。
- 楼宇设备不同于员工工作设备、游乐设备：本域只管电梯/暖通/配电/给排水消防等建筑机电设施。
- **工单主表口径（2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26 定稿：彻底分表）**：**设施工单与商户服务工单分属两张表、各自独立状态链，不并主表**——
  - **设施工单**走 V166 `facility_work_orders`，承接设备告警、巡检异常、平台发起与现场工作人员报修，状态链见 §1 `status` 行（九态）；
  - **商户服务工单**走 `merchant_service_work_orders` / `merchant_service_work_order_actions`（V022），**恢复为该域主写表**，采用 ENUM「商户服务工单状态」条的**独立七态状态链**：已提交 / 已派单 / 处理中 / 待商户确认 / 已处理待关闭 / 已关闭 / 已取消（DB 侧为独立 ENUM／CHECK，不与设施工单九态混用）；
  - 看板按 ENUM「工单时效」要求**分行对照展示**，两类工单不合并计数、不互相映射状态。
  - **本条撤销 2026-08-04 F2 批的「统一主表、商户服务工单以 `source=merchant_self` 同表承接」口径**（该口径当时为待裁决登记项，现按用户裁决作废）。商户专区「我的工单」ADM-MER-004 继续走 `/api/v1/merchant-service/work-orders`，**无需向 `/api/v1/facility/work-orders` 收敛**；V022 的 7 值 CHECK 已存在，应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`），`cancelled` 只被识别为终态，无取消入口、当前不可达；当前待补齐 `cancelled` 的合法取消入口，并对齐 V166 `merchant_self`、历史注释及看板分行指标，见 §3/§6 C。同一口径见 `real-estate-full-chain-domain-prd-v0.1.md` §3、`merchant-portal-zone-prd-v0.1.md` §3/§6。
- 能耗抄表独立归 `energy` 域；**（2026-08-04 第三轮用户裁决 UD-04 / DEC-R6-12 定稿）** 楼宇巡检类型**四类**——安防、消防、设施设备、环境卫生，**删除「能耗抄表」**，能耗抄表一律走**能耗域独立抄表流程**（按 PGM「巡检计划支持按类型模板自动排、主管手动排、按点位路线排三种」口径收口）。ENUM「楼宇巡检类型」条已同批改为四类，原五类口径作废。**DB CHECK 现为 4 值**（`security`/`fire`/`facility`/`environment`），与本裁决一致，**无需改造**。
- 不做：具体 IoT 协议接入、第三方远程表厂商对接、备件库存采购闭环；这些只保留字段或上下游连接点。

## 1. 业务对象与状态枚举（对齐 HTML §十；当前原值与目标待建分列）
| 对象 | 枚举/字段 | 当前 DB CHECK / 服务校验原值 | 目标业务口径与待建缺口 |
|---|---|---|---|
| 设施设备 `facility_equipment` | `equipment_type` | `elevator` / `hvac` / `power` / `water_fire` | 电梯扶梯 / 暖通空调 / 配电供电 / 给排水消防 |
| 设施设备 `facility_equipment` | `status` | `normal` / `alarm` / `fault` / `maintenance` / `disabled` / `retired` | 正常 / 告警 / 故障 / 维保中 / 停用 / 报废 |
| 设施设备 `facility_equipment` | `alarm_source` | `manual` / `iot` | 人工上报 / 远程或物联自动采集；仅告警时填写；后台状态变更入口为员工人工上报只记 `manual`，`iot` 留给物联设备真实接入后的记录 |
| 工单策略 `facility_work_order_policy`、工单 `facility_work_orders` | `work_order_type` | `repair` / `cleaning` / `complaint` / `rectification` / `inspection` / `other`（V166 与 `FacilityWorkOrderService.TYPES` 均为 6 值） | **业务目标（ENUM「物业工单类型」）**：报修、保洁、投诉、整改、巡检、**内部协作工单**（内部部门间发起与流转）、其他服务请求。待实现：V166 的两处 CHECK、服务校验与策略种子补内部协作工单；本轮不改迁移或代码。 |
| 工单策略/工单 | `priority` | `urgent` / `high` / `normal` | 用于匹配 SLA「类型 × 优先级」 |
| 工单策略 | `acceptance_by` | `handler` / `supervisor` / `reporter` | 处理人完成即结 / 主管验收 / 提单方确认。复杂类（大型设备报修）的主管验收＋提单方确认两道不是可配的验收方式：关联了设备的报修单一律两道，没关联设备的普通报修按此配置（HP-F02，V531；第十二批操作员裁决 2026-10-21 第 12 条，见 §3） |
| 工单 `facility_work_orders` | `source` | `merchant_self` / `platform` / `equipment_alarm` / `inspection`（V166 当前 CHECK；**不含** `customer`） | **目标待建值**：补 `customer`，使客人端投诉由客人自行提交、客人即提单方且关闭须其确认。`customer` 不是当前 DB 原值；CHECK 扩值列入排期，见 §6 E。 |
| 工单 `facility_work_orders` | `status` | `pending_assign` / `assigned` / `processing` / `pending_review` / `pending_acceptance` / `accepted` / `closed` / `suspended` / `overtime` | 待派单 / 已派单 / 处理中 / 待回访 / 待验收 / 已验收 / 已关闭 / 已挂起 / 已超时；`accepted`＝主管已验收、待提单方确认（HP-F02，V531 起可达）。**（2026-08-04 第三轮用户裁决 UD-05 / DEC-R6-13 定稿）** 九态**按工单类型分支适用，不是全类顺序链**——①**保洁等无争议类**：处理人完成即结（`processing` → `closed`）；②**投诉类**：须**提单方确认**（`processing` → `pending_review` → `closed`），客人端投诉的提单方即客人；③**复杂类**（大型设备报修等）：须**主管验收 ＋ 提单方确认双闸**——现行实现：关联设备的报修完工进 `pending_acceptance`，主管验收与提单方确认先后不限，主管先验收为 `accepted`，两项齐全才 `closed`，不经 `pending_review`。原「待回访 → 待验收全类顺序链」读法作废。「复杂类」判定口径按第十二批操作员裁决 2026-10-21 第 12 条为关联设备的报修，见 §3 |
| 巡检模板/任务 | `inspection_type` | `security` / `fire` / `facility` / `environment` | 安防 / 消防 / 设施设备 / 环境卫生；**能耗抄表不属巡检类型**，走能耗域独立抄表流程（2026-08-04 第三轮用户裁决 UD-04 / DEC-R6-12 定稿为四类，ENUM「楼宇巡检类型」条已同批改为四类，见 §0）。DB CHECK 4 值与裁决一致，无需改造 |
| 巡检模板 | `abnormal_mode` | `light` / `to_work_order` | 轻记录闭环 / 转整改工单闭环 |
| 巡检任务 `facility_inspection_tasks` | `status` | `pending` / `in_progress` / `normal` / `abnormal` / `rectifying` / `rechecked` / `closed` | 待执行 / 执行中 / 正常 / 发现异常 / 整改中 / 已复检 / 已关闭 |
| 巡检任务 | `schedule_mode` | `template` / `manual` / `route` | 模板自动排 / 主管手动排 / 点位路线排 |
| 巡检记录 `facility_inspection_records` | `result` | `normal` / `abnormal` | 正常 / 异常 |
| 设备维保 `facility_equipment` | `maintenance_type` | V165 当前**无该字段或 CHECK** | **业务目标（ENUM「设备维保类型」逐字）**：定期保养、例行巡检、故障维修、安全检测、部件更换。待实现：补字段、值域约束与维保记录/工单关联；本轮不改迁移或代码。 |
| 能耗表计 `energy_meters` | `meter_type` | `electricity` / `water` / `gas` / `heat_cold` | 电表 / 水表 / 燃气表 / 冷热量表 |
| 能耗表计 `energy_meters` | `gather_object` | `public_area` / `merchant` / `equipment_circuit` | 公共区域 / 商户 / 设备或回路 |
| 能耗表计/读数 | `read_mode` | `remote` / `manual` | 远程自动读 / 人工抄表 |
| 抄表读数 `energy_meter_readings` | `status` | `pending` / `recorded` / `reviewed` / `abnormal` | 待抄 / 已抄录 / 已审核 / 异常 |
| 抄表读数 `energy_meter_readings` | `abnormal_reason` | `surge` / `drop` / `zero` / `reverse` | 突增 / 突降 / 零读数 / 读数倒走 |
| 合同 `contracts` | `energy_fee_basis` | 写入与 V480 DB CHECK 仅接受 `none` / `by_contract` | 无代收约定 / 独表实际费用代收；公区及水电损包含在物业费中。详见§5。 |

> **工单分表口径（2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26 定稿，见 §0）**：上表的 `facility_work_orders`（V166）只是**设施工单**主表；**商户服务工单彻底分表**，主写 `merchant_service_work_orders`（V022），采用 ENUM「商户服务工单状态」条的**独立七态状态链**（已提交/已派单/处理中/待商户确认/已处理待关闭/已关闭/已取消），并按 ENUM「工单时效」在看板上**分行对照**展示。上表 `facility_work_orders.status` 九态**不适用于商户服务工单**。原 F2 批「统一主表、`source=merchant_self` 同表承接」口径已撤销。

## 2. 设施设备台账（V165）
**业务规则**
- 建台账时记录名称、4 分类、安装空间、品牌、型号、供应/厂商、责任人、项目/商户归属；初始状态固定为 `normal`。
- **设备维保类型业务目标**：逐字采用 ENUM「设备维保类型」的**定期保养、例行巡检、故障维修、安全检测、部件更换**。当前 V165 与 `FacilityEquipmentRequest` 均没有维保类型字段/约束，也没有维保记录与设施工单的关联，列入 §6 G 待建。
- 每台设备单独配置 `maintenance_cycle_days`。下次维保到期 `next_maintenance_due` 由 `install_date` 或最近一次 `last_maintenance_at` 加周期派生并固化；`GET /maintenance-due` 读取到期清单。
- **到期维保单业务目标**：到期提醒自动生成维保单。当前 `FacilityEquipmentService` 只提供到期清单查询和 `maintenance-done` 回写，未见到期扫描/自动创建设施工单的服务或定时任务，列入 §6 I 待建。
- 状态改为 `alarm` 时必须带 `alarm_source`；此入口为员工人工上报，只收 `manual`，带 `iot` 或缺失拒绝（`VALIDATION_FAILED`，不落接警单），后台工作台不再提供告警来源下拉、固定按手工提交；物联来源须经真实设备接入，不能自行声明（第十四批 OBS-13-06，PGM「告警来源支持人工上报与远程/物联自动采集两种」）；离开 `alarm` 会清空告警源、备注和告警时间。
- 状态改为 `alarm` 或 `fault` 时必须带优先级 `priority=urgent|high|normal`（紧急/高/普通，由上报人或物联网上报当场选；第十二批操作员裁决 2026-10-21 第 1 条），缺失或取值不对拒绝（`VALIDATION_FAILED`）。
- 状态改为 `alarm` 或 `fault` 时发布 `FacilityEquipmentAlarmedEvent`（带设备项目、位置、四组台账分类、负责人与上报优先级），工单服务监听后自动生成 `repair` 工单：优先级取上报优先级，项目与位置取设备台账，`source=equipment_alarm`，标题为“设备告警: 设备名”；同一事务落接警单并首次派发（见 §3「自动告警接警」实现现状）。
- 维保完成调用 `maintenance-done`：写 `last_maintenance_at=now`，状态回 `normal`，按维保周期重算 `next_maintenance_due`。
- **设备报废（HP-F03，V532）**：BSM「楼宇设备报废由设备所有权主体确认」。报废不能经状态变更直接置（`FACILITY_EQUIPMENT_SCRAP_APPROVAL_REQUIRED`），须提交报废申请（报废原因、处置方式，可指定最终审批人）进统一审批（`action_type=facility.equipment.scrap`，审批策略 `facility_equipment_scrap` 恒阻断、发起人沿直接上级链到其指定的最终审批人；设备台账只由平台／项目物业方维护，所有权主体即该方）；审批通过才把设备置 `retired` 并记批准人（终审人），驳回设备照常在用、可重新申请；同一设备同时只能有一张待审批申请。已报废设备不能经状态变更、维保完成或维修工单关闭改回在用（`FACILITY_EQUIPMENT_RETIRED`）。
  - 报废单 `facility_equipment_scraps` 分别记三项事实：审批（状态、批准人、批准时间、处置方式）；实物处置（批准后单独登记，须附已上传且绑定本报废单的凭证，`EVIDENCE_REQUIRED` / `EVIDENCE_BINDING_MISMATCH`，凭证只收真实文件上传）；款项（批准后财务登记一次残值与损失：残值记对回收方的应收 `source_type=facility_equipment_scrap`，收款走应收应付既有登记收付款，收足才算结清；损失记内部核算 `source_type=facility_equipment_scrap_loss`，登记即确认；两项均关联本报废单并归原设备项目）。清单按审批／实物处置／款项分列，实物已处置且款项已结清才算全部收尾；标报废不等于实物处置或款项完成。
  - 借入设备的报废按库存借用流程（FS-019）的双方确认、实物归还与赔偿款分别收尾办理，不进本表。
  - 后台楼宇运营工作台：设备行「申请报废」，状态变更下拉不再提供「报废」；「⑤ 报废处置」页签列审批、实物处置、款项与是否全部收尾，可登记实际处置（先上传凭证）与财务登记残值／损失（按元录入，`finance.ar_ap.manage`）；审批人待办识别「楼宇设备报废」。
  - 依据：BSM「楼宇设备报废由设备所有权主体确认」；ENUM「设备报废的审批、实际处置及款项记录，按业务系统地图的楼宇机电设备报废规则执行」；决策台账 2026-10-17 第 13 项 Q19。

**关键端点（实际 Controller）**
- `GET /api/v1/facility/equipment`
- `GET /api/v1/facility/equipment/maintenance-due`
- `POST /api/v1/facility/equipment`
- `POST /api/v1/facility/equipment/{equipment_id}`
- `POST /api/v1/facility/equipment/{equipment_id}/status`
- `POST /api/v1/facility/equipment/{equipment_id}/maintenance-done?idempotency_key=...`
- `POST /api/v1/facility/equipment/{equipment_id}/scrap-requests`（报废申请，`facility.equipment.manage`）
- `GET /api/v1/facility/equipment-scraps`（报废处置清单，`facility.equipment.manage` 或 `finance.ar_ap.manage`）
- `POST /api/v1/facility/equipment-scraps/{scrap_id}/disposal`（登记实际处置，`facility.equipment.manage`）
- `POST /api/v1/facility/equipment-scraps/{scrap_id}/finance`（财务登记残值／损失，`finance.ar_ap.manage`）

**迁移 / 表 / 权限**
- 迁移：`V165__facility_equipment.sql`；报废 `V532__facility_equipment_scraps.sql`
- 表：`facility_equipment`；报废单 `facility_equipment_scraps`
- 权限：`facility.equipment.manage`（`domain=platform`、`risk_level=medium`、`requires_approval=false`）

## 3. 设施工单（V166）
**业务规则**
- **自动告警接警与休息边界**：当班且承担接警任务的人员主动确认“已收到并接手”，送达、响铃或接听均不等于确认；未确认按已配置告警类型、优先级和当班名单升级，自动语音仅催接正在当班且承担该任务的人。设备负责人休息时只收系统消息，下一工作时段确认，休息期间不计响应超时、不电话催促。设备停用或继续使用按专业负责人及维保单位预先确认的故障规则处理，未覆盖由当班专业人员判定；不等于授权平台直接远程停机。涉及人身安全立即按应急流程处置，不等待负责人上班或系统确认。接警后继续派修和跟踪，换班未结事项由下一班明确接手。
  - **实现现状（HP-F01，V529）**：
    - 接警单：告警自动建工单的同一事务内落 `facility_alarms`（待明确接手 `awaiting_takeover` / 已明确接手 `taken_over`）。响应时限按「告警类型×优先级」矩阵 `facility_alarm_response_policies` 计：告警类型＝设备四组台账（电梯扶梯/暖通空调/配电供电/给排水消防），优先级紧急/高/普通，后台逐格配置（不预置出厂数值；保存按幂等键，失败记 `facility.alarm_policy_save_failed`）；该格未配置时拒绝上报（`POLICY_NOT_FOUND`）。
    - 接警值守安排：`facility_alarm_duty_assignments` 在已发布且未结束的班次上指定主接、备用、物业主管、项目负责人、专业处置人员及联系电话，项目取班次所属项目，同班同角色只能有一条有效安排；当班人沿顶班记录解析为此刻实际当班的人（与夜班值班经理同一份顶班解析），班次未开始或已结束＝休息，不联系。不做全天覆盖或缺岗补位。
    - 首次派发：当班主接逐人发员工系统消息、短信（`NotificationService`）并经外部语音服务端口 `VoiceCallSender` 呼叫（外部语音供应商接入前每次呼叫如实记呼叫失败 `VOICE_PROVIDER_NOT_CONFIGURED`、不记已拨出；短信供应商接入前短信不会显示已送出（总开关关时记未发送，开时记发送失败 `SMS_PROVIDER_NOT_CONFIGURED`），故此期间主接都按发送失败立即转当班备用），三条逐次写入 `facility_alarm_contact_attempts`、结果取通道真实返回（系统消息里优先级写紧急／高／普通，响应时限按本地时间显示）；此刻无当班主接，或每个主接短信未送出且语音呼叫失败，同一次上报内立即转当班备用并记转备用时间。设备负责人只收一条系统消息，不发短信、不打语音。
    - 明确接手：`POST /api/v1/facility/alarms/{alarm_id}/take-over` 是唯一改变接警状态的动作，只有此刻当班的主接、备用、物业主管或项目负责人可接，写 `facility_alarm_takeovers`（确认人、班次、角色、时间）；消息送达或已读、拨通、响铃、接听都不改接警状态。接手不改工单状态，接手人按原派单流程派维修；接警单仍待明确接手（含换班后回到待接手、须下一班明确接手）时，该自动工单拒绝派单（`ALARM_TAKEOVER_REQUIRED`，被拒记 `facility.work_order_assign_failed`，依据 BSM「接警不等于维修完成，接手后派相应维修人员并跟踪处理和验收」）。
    - 超时升级（V529＋定时作业 `FacilityAlarmEscalationJob`，prod 开启、默认每分钟一轮）：待接手且已到响应时限的，轮次加一，向此刻当班的主接、备用、物业主管、项目负责人发系统消息＋短信＋语音，响应时限按同一格顺延，下一轮按届时的当班范围继续升级；值守安排内此刻无人当班的，不联系安排外的人、不叫醒休息人员，只留「无人接手」事实（`facility.alarm_unclaimed`）。任何一轮都不把告警或工单置为已响应、已完成。
    - 换班交接：已接手的告警，接手人那一段班次（原班或顶班时段）结束，或领班给那一段追加了此刻生效的临时顶班（班中顶班）而工单未关闭的，回到待明确接手、清持有人（接手历史留在 `facility_alarm_takeovers`），重新计响应时限并向此刻当班主接派发（失败转当班备用）；下一班须经「已收到并接手」明确接手（记为换班接手），查看清单不算接手。工单已关闭的不进交接。
    - 故障处置规则（V530 `facility_fault_handling_rules`）：按单台设备或按项目×设备四组台账分类登记故障场景与处置要求，须设备专业负责人（平台员工）与维保单位（外包维保账号，限本项目）两方确认才生效，可停用。告警落单时无生效规则覆盖的标「未覆盖」，并向此刻当班的专业处置人员发系统消息＋短信（不在语音催接之列）。处置要求记录 `POST /api/v1/facility/alarms/{alarm_id}/disposition`：按覆盖该设备的生效规则处理的由此刻当班的接警值守人员记所按规则，未按已确认规则处理的须由此刻当班的专业处置人员记判定（停用或继续使用等）；每张接警单只记一次，只落记录，不调用任何设备控制、不改设备状态，不等于授权平台远程停机。
    - 前端与手机入口：后台楼宇运营工作台「④ 设备接警」页签含我的接警（「已收到并接手」、记录处置要求）、接警记录（联系与接手明细、覆盖规则）、接警值守安排、告警类型×优先级响应时限、故障处置规则登记/专业负责人确认/停用；设备状态变更表单在告警/故障时须当场选优先级。外包维保工单页可对本项目待确认规则作维保单位确认。员工手机首页「我的接警（已收到并接手）」列出待接手与本人持有的告警并可明确接手，待办与消息中心列待接警；公告「确认已读」不触发接手。
    - 依据：BSM「连接物业服务，设备告警与巡检异常自动生成物业工单，派单到人」条；第十二批操作员裁决 2026-10-21 第 1 条。
- **系统事件提单确认**：设备或点位在岗责任人承接确认；责任人不在岗或本人负责此次维修，由物业主管指定其他人员并留痕。复杂工单仍须主管／物业验收与提单责任人确认两项通过才关闭；接警不等于维修完成。
- 当前设施工单类型为 6 类、优先级 3 类；内部协作工单是 §1 所列业务目标及待建缺口。SLA 不硬编码在服务里，按 `facility_work_order_policy(work_order_type, priority)` 查响应时限和处理时限，唯一约束 `uq_fwop_type_priority`。
- 默认策略（**2026-08-04 对齐 BSM「运营细则（已确认）」条**）：保洁/巡检/其他按**处理人完成即结**（无争议类）；投诉按**提单方确认**（一律须提单方确认才能关闭）；**报修/整改按主管验收**，其中**复杂类（大型设备报修）须「主管/物业验收**并且**提单方确认」双闸——两者都通过才能关闭，缺任一不得关闭**。后台可通过 `POST /policies` 调整。
  - **实现现状（HP-F02，V531；第十二批操作员裁决 2026-10-21 第 12 条「关联设备的报修才两道」）**：关联了设备（`equipment_id` 非空）的报修单（含设备告警自动生成的）一律走主管验收＋提单方确认两道，不看所配验收方式；没关联设备的普通报修按后台配置的 `acceptance_by`（种子为主管验收），`POST /policies` 的验收方式只收 `handler`／`supervisor`／`reporter`；整改维持主管验收。两道工单完工进 `pending_acceptance`；主管／物业验收（`verified_at`/`verifier_id`）与提单方确认（`reporter_confirmed_at`/`reporter_confirmed_by`，满意度由提单方评）分别留存、先后不限：主管先验收为 `accepted`（已验收、待提单确认），两项齐全才 `closed` 并回写设备；提单方确认只能一次。`accepted` 计入活动态，未关闭的继续按原处理时限标超时；每次完工开始新一轮验收，旧确认作废。
  - 自动生成的大型维修工单（`source=equipment_alarm`）提单确认承接人 `confirmer_id` 取设备（点位）责任人，提单方仍记为系统；确认时承接人须为本项目在岗员工（有效员工账号且停岗／离职未生效，休息不算不在岗）、不是本次维修人，并由其本人确认（否则 `CONFIRMER_DESIGNATION_REQUIRED` / `CONFIRMER_MISMATCH`）；承接人缺失、不在岗或就是本次维修人时，物业主管经 `POST /{work_order_id}/confirmer` 指定本项目其他在岗员工并留痕（审计记原承接人（设备未登记责任人时无原承接人）、新承接人与原因），其余情形不允许另行指定（`CONFIRMER_DESIGNATION_NOT_ALLOWED`）。提单确认（`/review`）与退回返工（`/rework`）：持 `facility.work_order.manage`，或本人即该自动工单（或下条员工手工建的关联设备报修单）的提单确认承接人（凭承接身份，不需另持 manage）；两者都不是的 403（提单确认被拒记 `facility.work_order_review_failed`）。退回返工、指定确认人按幂等键防重，同键重放返回第一次的结果。主管验收、提单确认、退回返工、指定确认人以及开始、完成、挂起、恢复、转派读工单时都锁行，同时操作串行、后提交的一方按最新处理人与状态校验，不以旧数据覆盖转派等改动。
  - 员工手工建的关联设备报修单（第十二批操作员裁决 2026-10-21 第 15 条「提单员工本人（推荐）」：「和告警单一样，提单员工就是承接人，在手机「我的工单」里确认或退回；提单人就是本次维修人时，由物业主管指定他人并留痕」）：后台建单（`POST /api/v1/facility/work-orders`）为关联设备的报修、提单方类型为员工（`reporter_type=staff`）的，提单员工＝在后台点「新建工单」的当前登录员工（服务端取当前登录账号记为 `reporter_id`，不取请求体里的 `reporter_id`；后台建单表单本无提单人输入），建单即记为提单确认承接人 `confirmer_id`；承接人在岗按工单项目判，建单未带项目的同告警单取设备台账的项目。此类单与自动工单同一套承接规则：提单确认只许承接人本人做（持 manage 的旁人 `CONFIRMER_MISMATCH`），主管验收照旧；承接人不在岗或就是本次维修人（派单或转派给提单人本人）时须由物业主管经 `/confirmer` 指定本项目其他在岗员工并留痕（`CONFIRMER_DESIGNATION_REQUIRED`，审计原因 `is_repairer`／`not_on_duty`／`missing`）；进承接人手机「我的工单」的待我提单确认。实现：`FacilityWorkOrderService.create`／`createInternal`／`confirmerBound`／`review`／`rework`／`designateConfirmer`。
  - 员工自有手机「我的工单」（第十二批操作员裁决 2026-10-21 第 13 条）：`GET /api/v1/facility/work-orders/assigned-to-me`（`facility.work_order.view_assigned`）在派给本人的工单之外，一并列出「待我提单确认」的单（本人是提单确认承接人、关联设备的报修、提单确认未做、待验收或已验收）；点选后带「确认」「退回返工」两个按钮，分别调 `/review`（提单确认）与 `/rework`（须填不通过原因），承接人凭承接身份即可操作，做完即不再列出。
  - 验收不通过（第十二批操作员裁决 2026-10-21 第 2 条「退回原维修人返工」）：待验收或已验收时经 `POST /{work_order_id}/rework` 填不通过原因，工单回 `processing` 由原维修人继续修，另一方已做的确认作废，修好后两方重新确认；处理时限不重算、超时照常升级。裁决所称换人用「转派」（见下条）。第十二批操作员裁决 2026-10-21 第 16 条「只许承接人本人（推荐）」：设备告警自动工单与员工手工建的关联设备报修单主管已验收（`accepted`）后只剩提单确认这一道，退回返工也只许承接人本人（同提单确认的承接人校验：旁人 403 `CONFIRMER_MISMATCH`，承接人不在岗或就是本次维修人 409 `CONFIRMER_DESIGNATION_REQUIRED`），被拒记 `facility.work_order_rework_failed`；待验收时主管验收不通过的退回照旧。实现：`FacilityWorkOrderService.rework`。
  - 转派（第十二批操作员裁决 2026-10-21 第 9 条「补转派」）：已派单或处理中时，物业主管（持 `facility.work_order.manage`）经 `POST /{work_order_id}/reassign` 填新处理人与原因（原因必填）改派他人；只换处理人，状态、响应／处理时限、派单时间与超时标记都不动（时限不重算、超时照常升级）；按幂等键防重，成功留痕原处理人、新处理人和原因（`facility.work_order_reassigned`），被拒记 `facility.work_order_reassign_failed`；不能改派给原处理人。后台工单行在已派单／处理中显示「转派」，新处理人填在处理人 id、原因填在转派原因。依据：BSM「派单、转派、升级、关闭都要留痕」。
  - 后台工作台：验收方下拉为处理人／主管／报告人（报修默认主管）；工单行按状态显示验收，关联设备的报修另显示提单确认、退回返工，设备告警自动工单与员工手工建的关联设备报修单另显示指定确认人；设备告警自动工单与员工手工建的关联设备报修单的「提单确认」只给承接人本人且其不是本次维修人时显示，持 manage 的旁人与身兼本次维修人的承接人不显示（后端对其分别 403 `CONFIRMER_MISMATCH`、409 `CONFIRMER_DESIGNATION_REQUIRED`，后者由物业主管指定确认人；BSM L1385、裁决第 15 条）；这两类单已验收后的「退回返工」同样只给不是本次维修人的承接人本人显示，待验收时照常显示（裁决第 16 条）；明细列主管验收人／时间、提单确认承接人、提单确认人／时间。
  - 依据：BSM「复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭」；ENUM「复杂类（大型设备报修等）须主管验收＋提单方确认双闸」；DEC-R3B-2026-08-04-25、DEC-R6-13；决策台账 2026-10-17 第 12 项 Q17；第十二批操作员裁决 2026-10-21 第 2、12、13、15、16 条。
- 创建工单时根据策略写 `response_due_at`、`handle_due_at`；当前 `mark-overdue` 仅扫描超期未关闭设施工单并置 `overtime=true`。**业务目标**是按响应/处理时限超时后自动通知并升级主管；当前缺通知、主管接收人解析、升级留痕和设施工单定时触发，列入 §6 H 待建。
- 设施工单的 `source` 当前仍允许 `merchant_self`；但现行业务裁决是商户服务工单由 V022 独立表承接，设施工单与商户服务工单彻底分表、各自状态链。故 `merchant_self` 在 V166 的当前允许值是待对齐缺口，不把它作为同表承接的业务口径；整改与巡检仍由平台、设备告警或巡检异常发起。
- 主流程：`pending_assign` → `assigned` → `processing`。完成时关联了设备的报修单一律进 `pending_acceptance`（两道：主管先验收为 `accepted`，两项齐全才关闭）；其余按 `acceptance_by` 分流：`handler` 直接 `closed`，`supervisor` 进 `pending_acceptance`，`reporter` 进 `pending_review`。
- 单闸下 `accept` / `review` 可记录 `satisfaction` 1-5，关闭时写 `verified_at`、`verifier_id`；两道（关联设备的报修）下主管验收（`accept`）写 `verified_at`／`verifier_id`、不评满意度，提单确认（`review`）写 `reporter_confirmed_at`／`reporter_confirmed_by` 并由提单方评满意度。关闭时对关联 `equipment_id` 的设备回写 `normal`。
- 支持 `suspend` / `resume`；只有活动态可挂起。恢复时有处理人的回 `processing`；待派单时挂起、没有处理人的回 `pending_assign`，派单仍只走 `assign`（设备告警自动工单仍须接警单明确接手才能派单）。
- 事件解耦：设备 `alarm/fault` 自动转 `repair`（优先级取上报优先级，项目与位置取设备台账）并落接警单；巡检异常 `to_work_order` 自动转 `rectification/normal` 并把 `work_order_id` 回写巡检任务。

**工单用料与费用承担方（2026-08-04 对齐 BSM「派单到具体人员或班组」条）**
- 工单**用料必须从指定库房出库**，并**按当前平均进货价计成本**（平均进货价口径：每次新进货后把新到的货与原有库存合并算出新的平均单价，出库即按该均价计成本）。
- 工单上**必须标明费用承担方**：**公区设施与楼宇主体设备**的用料**由物业承担**；**商户自有设施、以及因商户责任导致损坏**的用料**由该商户承担，并自动生成一笔应收进入其费用账单**。
- **承担方有争议的，工单挂起转上级判定**（复用既有 `suspend`/`resume`），**判定结果与依据留痕后才能结案**。
- **当前实现缺口**：`facility_work_orders`、`FacilityWorkOrderRequest` 与 `FacilityWorkOrderRecord` 未承载指定库房、出库记录、当前平均进货价、费用承担方、争议判定结果/依据或关联应收；当前 `suspend`/`resume` 只有工单状态操作，未形成上述物料和费用闭环，列入 §6 L 待建。

**客诉闭环机制（2026-08-04 对齐 BSM「运营细则（已确认）」条）**
- **工单由客户关怀部负责处理**；**响应时限与处理时限对商户公开**（即 `facility_work_order_policy` 的「类型 × 优先级」时限对商户侧可见）。
- **投诉类一律须提单方确认才能关闭**；提单方在后台配置的时限内未确认的**视为默认关闭**（业务参数，按工单类型可配），但**单独统计为「未确认关闭」**，不与正常确认关闭混计。
- **所有客诉必须有跟进、有记录、有结果，直至形成闭环**；**客诉须经客户关怀部确认后方可视为形成闭环**，未经其确认的一律不算闭环。
- **系统每周自动生成客诉周报**，列明当周客诉的**笔数、类型、处理进度与闭环情况**——**店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人**。
- **升级铁律**：**一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人**；**是否属于同一类由客户关怀部认定**（人工认定，系统只负责识别候选与推送）。
- **当前实现缺口**：`FacilityWorkOrderService` 未见客户关怀确认、客诉周报生成/投递、同类投诉候选识别或“三个不同的人”升级及推送逻辑，列入 §6 M 待建。
- **实现现状（2026-10-22 第十三批 ROOT-03 B 段 B-account，V540）**：工单来源闭集加 `customer`（客人在官方 App、小程序提交，客人即提单方，只经客人端建单，后台新建不收此来源）；客人投诉附件（照片、视频、语音）为真实文件，后台按工单查看权限受控查看。客人投诉转为关闭时须记录客人反馈（`customer_feedback`，缺则 409 `COMPLAINT_CUSTOMER_FEEDBACK_REQUIRED`），不要求客人在端上确认（台账「两类投诉稿」已批准：BSM「客户关怀部记录问题解决方案及客人反馈后可以关闭」，取代上条「投诉类一律须提单方确认才能关闭」对消费客人投诉的适用）；满意度由客人本人在客人端评（BSM「满意度在回访/验收时由提单方评分」），员工回访、验收不改写。后台工作台关闭弹窗对客人投诉加「客人反馈」必填框、明细可查看客人附件。客诉周报与三人升级仍为缺口（§6 M）。

**关键端点（实际 Controller）**
- `GET /api/v1/facility/work-orders`
- `GET /api/v1/facility/work-orders/overdue`
- `GET /api/v1/facility/work-orders/assigned-to-me`（员工自有手机「我的工单」，`facility.work_order.view_assigned`；含待我提单确认的单）
- `GET /api/v1/facility/work-orders/{work_order_id}`
- `POST /api/v1/facility/work-orders`
- `POST /api/v1/facility/work-orders/{work_order_id}/assign`（设备告警自动工单须接警单已明确接手）
- `POST /api/v1/facility/work-orders/{work_order_id}/reassign`（已派单或处理中转派，`facility.work_order.manage`）
- `POST /api/v1/facility/work-orders/{work_order_id}/start`
- `POST /api/v1/facility/work-orders/{work_order_id}/complete`
- `POST /api/v1/facility/work-orders/{work_order_id}/accept`
- `POST /api/v1/facility/work-orders/{work_order_id}/review`（单闸回访；双闸下为提单方确认）
- `POST /api/v1/facility/work-orders/{work_order_id}/rework`（验收或提单确认不通过退回返工，`facility.work_order.manage`，或本人即设备告警自动工单、员工手工建的关联设备报修单的提单确认承接人；这两类单已验收后只许承接人本人，第十二批操作员裁决第 16 条）
- `POST /api/v1/facility/work-orders/{work_order_id}/confirmer`（指定提单确认人，`facility.work_order.manage`）
- `POST /api/v1/facility/work-orders/{work_order_id}/suspend`
- `POST /api/v1/facility/work-orders/{work_order_id}/resume`
- `POST /api/v1/facility/work-orders/mark-overdue`
- `POST /api/v1/facility/work-orders/policies`
- `POST /api/v1/facility/alarms/policies`（告警响应时限一格，`facility.alarm.manage`）
- `GET /api/v1/facility/alarms`（接警记录带逐次联系与接手明细，`facility.alarm.manage`）
- `GET /api/v1/facility/alarms/mine`（我的接警，`facility.alarm.take_over`）
- `POST /api/v1/facility/alarms/{alarm_id}/take-over`（明确接手，`facility.alarm.take_over`）
- `GET /api/v1/facility/alarm-duties`、`POST /api/v1/facility/alarm-duties`、`POST /api/v1/facility/alarm-duties/{duty_id}/cancel`（接警值守安排，`facility.alarm.manage`）
- `POST /api/v1/facility/alarms/{alarm_id}/disposition`（处置要求记录，`facility.alarm.take_over`）
- `GET /api/v1/facility/fault-rules`（平台侧 `facility.alarm.manage`；维保单位 `facility.fault_rule.confirm`，只看本项目）、`POST /api/v1/facility/fault-rules`、`POST /api/v1/facility/fault-rules/{rule_id}/retire`（`facility.alarm.manage`）、`POST /api/v1/facility/fault-rules/{rule_id}/confirm`（`facility.fault_rule.confirm`）

**迁移 / 表 / 权限**
- 迁移：`V166__facility_work_orders.sql`；双闸 `V531__facility_work_order_dual_confirmation.sql`（`facility_work_orders` 加 `confirmer_id`、`reporter_confirmed_at`、`reporter_confirmed_by`；验收策略不改）
- 表：`facility_work_order_policy`、`facility_work_orders`
- **商户服务工单表（分表，2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26）**：`merchant_service_work_orders` / `merchant_service_work_order_actions`（V022）是**商户服务工单的主写表**，与设施工单彻底分表、各自状态链；商户专区「我的工单」ADM-MER-004 继续走 `/api/v1/merchant-service/work-orders`（`MerchantServiceController` → V022），**不向 `/api/v1/facility/work-orders` 收敛**。V022 的 7 值 CHECK 已存在；应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`），`MerchantServiceService` 已实现创建、派单、处理、待商户确认、已处理待关闭、关闭和逾期打标/动作留痕；`cancelled` 只被识别为终态，无取消入口、当前不可达。仍待补齐 `cancelled` 的合法取消入口，并对齐 V166 现仍接受 `merchant_self`、其历史迁移注释还称 V022 停止写入、设施/商户工单看板分行对照及商户工单不显示首次响应指标。本轮只改 PRD 口径，**不改 CSV、不改代码、不改迁移**。
- 权限：`facility.work_order.manage`（`domain=platform`、`risk_level=medium`、`requires_approval=false`）
- 接警（V529 `V529__facility_alarm_takeover.sql`）：表 `facility_alarm_response_policies`、`facility_alarms`、`facility_alarm_duty_assignments`、`facility_alarm_contact_attempts`、`facility_alarm_takeovers`；权限 `facility.alarm.manage`、`facility.alarm.take_over`（`domain=facility`、`risk_level=medium`，授予现持 `facility.work_order.manage` 的角色与权限模板）
- 故障处置规则（V530 `V530__facility_alarm_fault_rules.sql`）：表 `facility_fault_handling_rules`，`facility_alarms` 加 `applied_rule_id`、`disposition_note`、`disposition_by`、`disposition_at`；权限 `facility.fault_rule.confirm`（`domain=facility`、`risk_level=medium`，授予现持 `facility.work_order.manage` 的角色与权限模板，另进外包维保权限模板 `tpl_outsourced_facility`（本项目范围，按 V181 限时授权范式））

## 4. 楼宇巡检（V167）
**业务规则**
- 巡检模板记录类型、周期、异常模式、检查清单、范围说明；`cycle_days` 为正数才参与自动排程。
- 三种排程方式：`template` 需要 `template_id`；`manual` 手动创建任务；`route` 必须提供 `route_points`。
- 定时排程由 `FacilityInspectionScheduleJob` 承担，开关为 `facility.inspection.scheduled.enabled=true`，默认固定延迟 `${facility.inspection.scheduled.fixed-delay-ms:300000}`。
- 现场留痕落 `facility_inspection_records`：照片 `photos`、二维码 `qr_code`、GPS `gps_lat/gps_lng`、检查清单结果 `checklist_result`、备注、异常说明、巡检人、记录时间。
- 正常记录：任务从 `in_progress` 写 `normal` 后立即 `closed`。
- 异常记录必须填 `abnormal_note`。模板 `abnormal_mode=light` 时写 `abnormal` 后直接 `closed`；`to_work_order` 时写 `abnormal` → `rectifying`，发布 `FacilityInspectionAbnormalEvent` 自动生成整改工单，整改完成后 `recheck` → `rechecked`，主管确认 `supervisor-confirm` → `closed`。
- 巡检闭环与游乐安全巡检分离；本域只覆盖楼宇安防/消防/设施设备/环境卫生。

**关键端点（实际 Controller）**
- `GET /api/v1/facility/inspections/templates`
- `POST /api/v1/facility/inspections/templates`
- `GET /api/v1/facility/inspections/tasks`
- `GET /api/v1/facility/inspections/tasks/{task_id}`
- `POST /api/v1/facility/inspections/tasks`
- `POST /api/v1/facility/inspections/tasks/{task_id}/start`
- `POST /api/v1/facility/inspections/tasks/{task_id}/records`
- `GET /api/v1/facility/inspections/tasks/{task_id}/records`
- `POST /api/v1/facility/inspections/tasks/{task_id}/recheck`
- `POST /api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm`

**迁移 / 表 / 权限**
- 迁移：`V167__facility_inspection.sql`
- 表：`facility_inspection_templates`、`facility_inspection_tasks`、`facility_inspection_records`
- 权限：`facility.inspection.manage`（`domain=platform`、`risk_level=medium`、`requires_approval=false`）

## 5. 能耗抄表与实际费用（HP-E01／E02／E03）

**计量与换表**
- 表计保留电、水、燃气、冷热量四类和公区、商户、设备回路三类归集。远程读表为主，真实设备接入仍须单独完成，员工抄表入口不能自行声明为远程；人工补充必须说明原因并上传绑定本表计的真实表底照片。人工抄表人、第二核表人、审核人均取当前登录的真实人员，不能代填他人身份。
- 表计启用先登记实际初值、时点、说明及照片；缺初值不得默认零。原始读数照录并保持异常、原起点及原用量为空，待第二人核实和主管确认。既有原读数不回填或覆盖。
- 换表关联旧表终值、新表初值、实际交接时点、原计费对象、凭据和旧段读数；旧表停用，新表按自己的初值计量。总表更换时保留分表交接事实与生效时间，历史总分表查询按当时绑定关系计算。不得跨表相减或恢复旧表继续计量。
- 永久停用登记真实终值、实际停用时点与照片，沿原读数链审核和处理异常，不强制创建替代表。已投入计量的表不能靠普通启用开关停用；原停用期仍核实全部计量及实际费用，后续期间停止催抄，永久停用表不得重新启用。未启用且没有读数的表仍可维护启用开关。
- 已审核的前段终值作为下段起点；上一段待核时不能录入下一段。异常阈值仍取既有同类型配置；不配置时不得猜默认价格或计量起点。表计维度允许同月多个真实计量区间。

**异常核实**
- 原四种异常原因不变；未知起点以异常状态及缺失原起点表示。正常读数由具备权限的人员审核，不另加必须第二人的条件；普通审核不能放行异常。第二人实际复抄或到场核表，提交核实终值、必要时的缺失起点、原因及真实照片，再沿现有主管复核链确认。
- 异常确认主管须为本项目在岗且持有异常确认岗位权限；原抄表人和本次核表人不能审批自身事实，审批时再次核验当前项目权限。原件、核实记录、驳回历史、审批单和附件均可查。
- 核实期限按项目审批，页面预填7天，上下限及真实能耗主管必须填齐。到期未核系统建立指向该主管的超期待办；主管在能源页发现待办并指派原抄表人之外的在岗复抄人。超期、缺配置或已经指派均不解除出账阻断。
- 同项目同商户当期任何未审核读数、应抄表缺记录或独表实际账单未齐，均阻止整期能源应收，不能只结正常的部分。看板的用量汇总仅纳入已审核的有效用量，异常列表包括缺初值记录。

**收费与往来**
- 公区及电损、水损只用于成本分析，相关费用包含在已批准的物业费中，不单独创建公摊、水电损或改名服务费应收。
- 商户独表仅按当期有效合同的实际费用代收。合同写入仅接受 `none` 与 `by_contract`，其中后者表示独表实费代收，不再读取固定 `rent_terms.energyFeeCent`，也不把用量乘默认单价。旧分摊合同不获新的收费授权。
- 获授权人员核对供能账单号、绑定本表计的真实凭据、已审核各段用量、实际费用与供能单位最后付款日，记录后锁定该表该期计量事实；不增加独立的实际费用审批流程。
- 齐单后在同商户同项目账期锁内按合同汇总实际费用。应收 `source_type=energy_fee` 并写入实际项目归属，按本项目财务可见、其他项目不得收款；来源键含商户、合同、账期，重试不重复生成。零费用保留核结记录而不造零元应收。实际单据和费用与应收互相可追溯；供能到期日沿用实际凭据。
- 能源为代收代缴往来，不纳入城市项目公司经营收入。物业费仍按既有独立合同流程生成，不能重收能源公摊。

**入口与存储**
- 既有表计、抄表、普通审核、总分表差额与能源结算端点保留。
- 新增 `POST /api/v1/energy/meters/{meter_id}/opening`、`/replacement`、`/retirement`；`GET /meters/{meter_id}/history` 及 `/attachments/{attachment_id}/file`。
- 新增 `POST /api/v1/energy/readings/{reading_id}/verification`、`/assignment`；`GET /api/v1/energy/escalations`。
- 新增 `GET/POST /api/v1/energy/actual-charges`、`GET/POST /api/v1/energy/projects/{project_id}/review-settings`。
- V480 将合同能源依据约束收敛为 `none` / `by_contract`，不自动改判合同。V478 建立启用、换表、父表交接、原始凭据、核实、实际费用、期限和升级指派记录；V482 仅增加永久停用时点及终段读数关联，凭据与人员沿用原读数证据链；原事实只追加，审核状态与首次账务关联仅按合法转换更新。历史读数缺证据不伪造回填。
- 管理、抄表及主管确认岗位按项目读取；各写入仍使用专用权限。证据上传额外要求附件权限与能源权限同时覆盖本表项目，下载只允许该表及实际换表关联原件。异常确认权限为 `energy.reading.approve`，不得通过自动授予全平台管理员绕过真实岗位。
- 页面为能源工作台：启用与换表、异常核表、独表实际费用、核实期限、计量原件与处置记录；主管确认沿现有主管复核页办理。代码完成不表示各项目初值、账单或配置已由运营人员确认。

## 5A. 施工完工、整改与验收（HP-F04）

- 供应商只可对指派给本人、处于施工中或整改中的报备上传真实文件并提交完工材料；未提供材料不能申请验收。整改重提须包含本轮新增材料，原材料与验收意见保留。
- 每次完工提交、验收通过或要求整改，分别记录操作者、时间、材料、意见及顺序号。验收引用当前所见的完工提交序号；没有待验收提交、引用旧序号或重复处置均拒绝，重复请求返回首次结果，不重复追加历史。
- 物业人员在“进场审批”与“施工验收”入口分别按对应权限及项目范围办理；供应商只能看本人报备。材料下载核对原报备、真实文件与操作者范围，不公开文件路径。
- `facility_site_entry_history` 由 V467 建立，只追加不可回改。报备状态更新与历史追加在同一事务，并锁定报备以避免整改、重新提交和验收互相覆盖。
- 端点沿用 `/api/v1/facility/site-entry-reports/{report_id}` 的提交和验收，新增 `/history`、真实文件 `/attachments` 与 `/attachments/{attachment_id}/file`；验收请求增加必填 `completion_sequence`。页面为 ADM-FAC-002、ADM-FAC-003 及供应商原进场报备页。

## 6. 待建一览（M08 现状登记）

> 本表保留各项历史归类；能源 A/D/J/K 的本批实现状态已同步，其他项按原登记；CSV 与业务 HTML 仅限 James V0.06 已批准的 PGM H1–H3 及入口注册表、规则账本、覆盖矩阵机械镜像。

| 编号 | 待建/待对齐项 | 现状 | 需要的动作 | 依据 |
|---|---|---|---|---|
| A | **电损、水损归入物业费** | HP-E01 已停止单列公摊应收 | 保持物业费按原合同处理，独表实费与物业费分开；不补造历史事实（见§5） | Q21；现行能源与物业费 HTML 条款 |
| B | **复杂类工单「主管验收 + 提单方确认」双闸** | HP-F02（V531）已实现：关联设备的报修单一律双闸，没关联设备的普通报修按配置验收（第十二批操作员裁决 2026-10-21 第 12 条） | 无（见 §3 实现现状） | BSM「运营细则（已确认）」条 |
| C | **商户服务工单分表独立七态**（2026-08-04 裁决改向：原「收敛到 V166」已撤销） | V022 的 7 值 CHECK 已存在；应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`）；`cancelled` 只被识别为终态，无取消入口、当前不可达 | 补齐 `cancelled` 的合法取消入口；对齐 V166 `merchant_self` 当前仍可写入、V166 历史注释称 V022 停止写入，以及设施/商户看板分行对照与商户单不显示首次响应指标（见 §3） | M08-CX-019/020；ENUM「商户服务工单状态」「工单时效」条 |
| D | **能源代收依据** | 写入、结算及 V480 DB CHECK 已收敛为 none/by_contract；后者仅独表实费 | 不自动改判历史合同，实际费用仍须真实独表与单据（见§5） | UD-07 / DEC-R6-28；HP-E01 |
| E | **工单来源补 `customer`** | `facility_work_orders.source` CHECK 现 4 值、无 `customer` | CHECK 扩 `customer`；客人端投诉直接落工单、客人为提单方、关闭须其确认（见 §1 `source` 行） | 第三轮用户裁决 UD-03 / DEC-R6-14；BSM 物业服务章·主要功能 |
| F | **工单九态分支语义落地** | HP-F02（V531）已实现复杂类双闸，`accepted` 可达（主管已验收、待提单确认） | 无（与本表 B 项同一改造，见 §3） | 第三轮用户裁决 UD-05 / DEC-R6-13；ENUM「物业工单状态」条 |
| G | **设备维保五类及记录承载** | V165 / `FacilityEquipmentRequest` 无 `maintenance_type` 字段、CHECK、维保记录或工单关联 | 按 ENUM 逐字补定期保养、例行巡检、故障维修、安全检测、部件更换的字段、值域与维保记录/工单关联 | GROK-M08-028；ENUM「设备维保类型」条 |
| H | **设施工单超时通知与升级主管** | `FacilityWorkOrderService.markOverdue` 仅置 `overtime=true`，无设施工单定时触发、通知、主管解析或升级留痕 | 补按响应/处理时限自动通知并升级主管的定时/服务/留痕链路 | GROK-M08-029；BSM「按响应时限和处理时限统计时效，超时自动升级通知主管」条 |
| I | **设备到期自动生成维保单** | `FacilityEquipmentService` 仅有 `maintenance-due` 查询与 `maintenance-done` 回写，无到期扫描/自动创建设施工单 | 补到期扫描、按设备生成维保单及幂等/留痕链路 | GROK-M08-030；PGM「设备分电梯扶梯、暖通空调、配电供电、给排水消防四组台账」条 |
| J | **远程抄表与公区归集** | HP-E01/E03 已实现人工原因和真实照片要求；公区不形成应收 | 运营按真实表计和接口接入；无数据不得估抄（见§5） | PGM 能耗条款 |
| K | **异常核表闸门** | HP-E03 已实现第二人、主管确认、原件留痕、期限审批、超期指派及整期暂缓 | 项目启用前补齐真实配置与岗位，未核异常始终不出账（见§5） | BSM 能耗抄表条款 |
| L | **设施工单用料与费用闭环** | 工单表、请求与记录无指定库房、出库、当前平均进货价、承担方、争议依据/结果或关联应收；现有挂起/恢复只改工单状态 | 补指定库房出库、当前平均进货价计成本、承担方、争议挂起后判定留痕及应收关联 | M08-CX-025；BSM「派单到具体人员或班组」条 |
| M | **客诉周报与三人升级** | `FacilityWorkOrderService` 无客户关怀确认、周报、同类投诉候选识别、三人升级或推送逻辑 | 补客户关怀确认、周报生成/投递、按月同类三个不同人识别与向三类负责人升级 | M08-CX-026；BSM「运营细则（已确认）」条 |

**已裁决清单（原 B 类五项，2026-08-04 第三轮用户裁决逐项定稿）**

1. **客人投诉进入工单域的落点** —— **已裁决（UD-03 / DEC-R6-14）**：`source` **补 `customer`**，客人端投诉直接落工单，客人即提单方、关闭须其确认。CHECK 扩值列入排期（见 §1 `source` 行、本节待建一览 E）。
2. **巡检类型是否含「能耗抄表」** —— **已裁决（UD-04 / DEC-R6-12）**：**四类**（安防、消防、设施设备、环境卫生），**删「能耗抄表」**，抄表走能耗域独立流程。DB 现为 4 值，与裁决一致，无需改造（见 §0、§1 `inspection_type` 行）。
3. **工单九态语义** —— **已裁决（UD-05 / DEC-R6-13）**：**按类型分支适用**——无争议类完成即结、投诉类须提单方确认、复杂类须主管验收 ＋ 提单方确认双闸；`accepted` 在复杂类分支下可达。双闸取值与「复杂类」判定口径改造列入排期（见 §1 `status` 行、本节待建一览 B/F）。
4. **商户服务工单是否与设施工单同表** —— **已裁决（UD-06 / DEC-R6-26）**：**彻底分表、独立七态**，撤销原「统一主表」口径；商户服务工单主写 V022，看板分行对照。V022 的 7 值 CHECK 已存在，应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`），`cancelled` 只被识别为终态，无取消入口、当前不可达；待补齐 `cancelled` 的合法取消入口，并对齐 V166 `merchant_self`、历史注释及看板指标（见 §0、§1 表后说明、§3、本节待建一览 C）。
5. **`by_area`/`by_usage` 存量合同处置** —— **已裁决（UD-07 / DEC-R6-28）**：研发环境**无存量数据**、测试数据可直接清理，**直接收敛 DB 取值**为 `none`/`by_contract`，不为历史迁移／回填单独立项（见 §1 `energy_fee_basis` 行、§5、本节待建一览 D）。

> 落地批次：`docs/prd-audit-fix-change-log-20260804.md` §12（第三轮裁决落地 R6-B）；裁决原文见 `docs/prd-audit/business-decision-ledger.md`「第三轮裁决」节。

## 7. M08 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-09-01 | 改 | 全文版本、§1、§5、§6 A | 版本升至 V1.01；电损费/水损费配置删除无出处的启用/停用开关，保留合法计费方式、数值、项目配置、强制上下限与超界拒绝 | M08-CX-006；James V0.06；BSM「能耗抄表」条 |
| 2026-09-01 | 改 | §1、§3、§6 C | 设施工单补 ENUM 的内部协作工单为待实现；商户服务工单改按 V022 两表、7 值 CHECK、状态动作与逾期留痕写实，未对齐项只列 V166 `merchant_self`、历史注释与看板指标 | M08-CX-011/019/020；GROK-M08-027/032；ENUM「物业工单类型」「商户服务工单状态」「工单时效」条 |
| 2026-09-01 | 改 | §1、§2、§6 G | 新增设备维保类型当前原值与目标待建分列；目标逐字采用定期保养、例行巡检、故障维修、安全检测、部件更换，登记 V165 字段/约束/关联缺口 | GROK-M08-028；ENUM「设备维保类型」条 |
| 2026-09-01 | 改 | §2、§3、§6 H–I | 补超时工单自动通知/升级主管和设备到期自动生成维保单的业务目标，并按 `markOverdue`、到期清单查询的当前实现登记具体缺口 | GROK-M08-029/030；BSM/PGM 楼宇运营条 |
| 2026-09-01 | 改 | §1、§5、§6 D/J | `public_area` 改为公区成本归集/分析；明确当前仍允许 `by_area`/`by_usage` 且仍生成公区商户应收，登记写入、结算与 CHECK 收敛待建；远程抄表为主、人工为异常/补充目标 | M08-CX-021/022/023；GROK-M08-033/034/037/038；BSM/PGM 能耗条 |
| 2026-09-01 | 改 | §5、§6 K | 异常抄表的照片、第二人复抄/到场核表、原因、主管确认、限期复核与账单暂缓均保留为业务目标；按 V168 DTO/服务登记未实现 | M08-CX-024；GROK-M08-039/040；BSM「能耗抄表」条 |
| 2026-09-01 | 改 | §3、§6 L–M | 用料指定库房、当前平均进货价、费用承担方/争议挂起，以及客诉周报/三人升级改按当前工单模型和服务缺口登记 | M08-CX-025/026；BSM「派单到具体人员或班组」「运营细则（已确认）」条 |
| 2026-09-01 | 改 | §1 全表 | DB CHECK 表头改为「当前原值」与「目标业务口径/待建缺口」分列，`customer` 仅列目标待建、不混入 V166 当前来源 CHECK | GROK-M08-047；V166 当前 CHECK |
| 2026-09-01 | 改 | 全文版本、§6 范围声明 | 版本升至 V1.02；范围声明改为不改代码、迁移与 OpenAPI，CSV/业务 HTML 仅限 James V0.06 已批准内容及机械镜像 | M08-FINAL-CX-02；Grok 交叉确认 |
| 2026-09-01 | 改 | 全文版本、§0、§3、§6 C/裁决 4 | 版本升至 V1.03；将 V022 当前实现校正为 7 值 CHECK 已存在、应用仅六态可达，`cancelled` 无取消入口并列入待建 | M08 最终双席复核（二次） |
| 2026-09-02 | 改 | 全文版本、重建动因 | 版本升至 V1.04；将 V165–V168 从“独立闭环”校正为独立领域骨架，并明确尚不等于权威业务闭环全部完成 | M08 Codex/Grok 全量终审双确认 |
| 2026-10-21 | 改 | 全文版本、§2、§3 | 版本升至 V1.08；告警/故障上报须当场选优先级，自动工单取上报优先级与设备项目、位置；登记接警单、告警类型×优先级响应时限、接警值守安排、首次派发留痕与转备用、明确接手、超时只在当班范围升级、换班须下一班明确接手、故障处置规则两方确认与未覆盖转当班专业处置人员判定、处置要求只记录不远程停机，及后台接警页签、外包维保规则确认与员工手机接手入口的实现现状 | HP-F01；BSM「连接物业服务，设备告警与巡检异常自动生成物业工单，派单到人」条；决策台账 2026-10-17 第 12 项 Q17A/B、Q17C、Q17D；第十二批操作员裁决 2026-10-21 第 1 条 |
| 2026-10-21 | 改 | §1、§3、§6 B/F | 同批 V1.08；复杂类大型设备报修「主管验收＋提单方确认」双闸（报修类一律）、两项分别留存齐全才关闭、已验收仍按原时效升级、自动大型维修工单提单确认由设备在岗责任人承接及物业主管指定确认人留痕、验收不通过退回原维修人返工的实现现状 | HP-F02；BSM「复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭」；第十二批操作员裁决 2026-10-21 第 2 条 |
| 2026-10-21 | 改 | §2 | 同批 V1.08；设备报废须所有权主体审批后才置报废、禁止直接改报废与报废后复活，报废单分记审批、实物处置凭证与残值／损失款项，财务关联原报废单与设备项目，后台报废处置页签的实现现状 | HP-F03；BSM「楼宇设备报废由设备所有权主体确认」；决策台账 2026-10-17 第 13 项 Q19 |
| 2026-10-21 | 改 | §2、§3 | 同批 V1.08（复核修正）：接手人班中被临时顶班即进换班交接；响应时限保存接幂等与失败审计；接警消息优先级与时限改中文／本地时间；自动大型维修工单的提单确认承接人不持 manage 也可确认与退回返工；双闸写操作锁行；故障处置规则确认权限对外包维保的授予方式写实为权限模板 | HP-F01、HP-F02 复核；BSM「换班时未结告警须由下一班明确确认接手」「由该设备或点位的在岗责任人承接提单确认」；第十二批操作员裁决 2026-10-21 第 1、2 条 |
| 2026-10-21 | 改 | §3 | 同批 V1.08：补设施工单转派（已派单或处理中由物业主管改派他人，原因必填，处理时限不重算、超时照常升级，留痕原处理人、新处理人和原因）；设备告警自动工单在接警单被明确接手前不派单；设备未登记责任人时物业主管指定提单确认人的留痕不含原承接人 | HP-F02、HP-F01 复核；BSM「派单、转派、升级、关闭都要留痕」「接警不等于维修完成，接手后派相应维修人员并跟踪处理和验收」；第十二批操作员裁决 2026-10-21 第 9 条 |
| 2026-10-21 | 改 | §1、§3、§6 B | 同批 V1.08：改为关联设备的报修单（含设备告警自动工单）才走主管验收＋提单方确认两道，没关联设备的普通报修按后台配置验收，验收方式不再有「主管验收＋提单方确认」可选、V531 不再改报修策略；员工自有手机「我的工单」一并列出待我提单确认的单，可确认或退回返工 | HP-F02；第十二批操作员裁决 2026-10-21 第 12、13 条 |
| 2026-10-21 | 改 | §1、§3 | 同批 V1.08（复核修正）：§1 工单状态行按现行实现改写（`accepted`＝主管已验收、待提单确认，两道先后不限、两项齐全才关闭、不经 `pending_review`，复杂类判定口径已定）；§3 两道下主管验收与提单确认分写字段；待派单时挂起的工单恢复回待派单（派单仍须过告警接手闸）；开始、完成、挂起、恢复与转派同锁工单行；派单、提单确认被拒留失败审计；退回返工、指定确认人同键重放返回首次结果；端点清单退回返工补承接人本人 | HP-F02 复核；BSM「接警不等于维修完成，接手后派相应维修人员并跟踪处理和验收」；BSM「派单、转派、升级、关闭都要留痕」；第十二批操作员裁决 2026-10-21 第 2、9、12 条 |
| 2026-10-21 | 改 | §3 | 同批 V1.08：员工在后台手工建的关联设备报修单，提单员工即提单确认承接人（建单即记，建单未带项目的取设备台账项目），提单确认与提单确认不通过退回只许承接人本人做、主管验收照旧，进其手机「我的工单」待我提单确认；提单人就是本次维修人时由物业主管指定他人并留痕；后台工作台对此类单只给承接人本人显示「提单确认」，另显示指定确认人 | HP-F02；第十二批操作员裁决 2026-10-21 第 15 条「提单员工本人（推荐）」 |
| 2026-10-21 | 改 | §3 | 同批 V1.08（复核修正）：提单员工＝在后台点「新建工单」的当前登录员工，服务端取当前登录账号记为 `reporter_id`（后台建单表单本无提单人输入，不取请求体里的值），建单即记为提单确认承接人；后台工作台的「提单确认」对设备告警自动工单与员工手工建的关联设备报修单只给不是本次维修人的承接人本人显示 | HP-F02；第十二批操作员裁决 2026-10-21 第 15 条「和告警单一样，提单员工就是承接人」；BSM L1385 |
| 2026-10-21 | 改 | §3 | 同批 V1.08：设备告警自动工单与员工手工建的关联设备报修单主管已验收后，退回返工只许承接人本人（旁人 403 `CONFIRMER_MISMATCH` 并记失败审计），待验收时主管验收不通过的退回照旧；后台工作台对这类已验收单的「退回返工」只给不是本次维修人的承接人本人显示；端点清单同步 | HP-F02；第十二批操作员裁决 2026-10-21 第 16 条「只许承接人本人（推荐）」 |
| 2026-10-21 | 改 | 版本、§2 | 版本升至 V1.09：第十三批 ROOT-05——外部语音与短信未接供应商时如实记失败、不记已拨出／已发送，接警首次派发因此即转当班备用的实现现状 | BSM「外部语音服务按既有第三方接入规则对接，不自研语音通道」；第十三批任务 ROOT-05 |
| 2026-10-22 | 改 | 版本、§3 | 版本升至 V1.10：第十三批 ROOT-03 B 段 B-account——工单来源加 customer（客人投诉）、客人投诉附件受控查看、客人投诉关闭须记录客人反馈、满意度由客人本人评的实现现状 | BSM「客户关怀部记录问题解决方案及客人反馈后可以关闭」；BSM「满意度在回访/验收时由提单方评分」；BSM「投诉与反馈：客人可在官方 App、小程序提交投诉，支持文字留言、照片、视频与语音」 |
| 2026-10-25 | 改 | 版本、§1、§2 | 版本升至 V1.11：第十四批 OBS-13-06——后台设备状态变更是员工人工上报，告警来源只收手工（`manual`），自报物联网（`iot`）拒绝且不落接警单，工作台去掉告警来源下拉；`iot` 取值与展示留给物联设备真实接入 | PGM「告警来源支持人工上报与远程/物联自动采集两种」「餐饮、零售、机器人、视频、设备都按接口资料逐步接」 |
