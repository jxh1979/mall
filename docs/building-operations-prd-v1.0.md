# 楼宇运营业务域 PRD（V1.06·据需求重建）

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
| 设施设备 `facility_equipment` | `alarm_source` | `manual` / `iot` | 人工上报 / 远程或物联自动采集；仅告警时填写 |
| 工单策略 `facility_work_order_policy`、工单 `facility_work_orders` | `work_order_type` | `repair` / `cleaning` / `complaint` / `rectification` / `inspection` / `other`（V166 与 `FacilityWorkOrderService.TYPES` 均为 6 值） | **业务目标（ENUM「物业工单类型」）**：报修、保洁、投诉、整改、巡检、**内部协作工单**（内部部门间发起与流转）、其他服务请求。待实现：V166 的两处 CHECK、服务校验与策略种子补内部协作工单；本轮不改迁移或代码。 |
| 工单策略/工单 | `priority` | `urgent` / `high` / `normal` | 用于匹配 SLA「类型 × 优先级」 |
| 工单策略 | `acceptance_by` | `handler` / `supervisor` / `reporter` | 处理人完成即结 / 主管验收 / 提单方确认。**（2026-08-04 口径，对齐 BSM「运营细则（已确认）」条）复杂类（大型设备报修）须「主管/物业验收**并且**提单方确认」双闸**——当前 3 值枚举**表达不了该组合，实现待对齐**（二选一：枚举扩展一个 `supervisor_and_reporter` 值，或在策略表加独立标志位 `require_reporter_confirm`）；本轮**不改 DB CHECK**，见 §3 默认策略与 §6 待建一览 |
| 工单 `facility_work_orders` | `source` | `merchant_self` / `platform` / `equipment_alarm` / `inspection`（V166 当前 CHECK；**不含** `customer`） | **目标待建值**：补 `customer`，使客人端投诉由客人自行提交、客人即提单方且关闭须其确认。`customer` 不是当前 DB 原值；CHECK 扩值列入排期，见 §6 E。 |
| 工单 `facility_work_orders` | `status` | `pending_assign` / `assigned` / `processing` / `pending_review` / `pending_acceptance` / `accepted` / `closed` / `suspended` / `overtime` | 待派单 / 已派单 / 处理中 / 待回访 / 待验收 / 已验收 / 已关闭 / 已挂起 / 已超时；`accepted` 为 DB 保留值，当前服务验收后直接写 `closed`。**（2026-08-04 第三轮用户裁决 UD-05 / DEC-R6-13 定稿）** 九态**按工单类型分支适用，不是全类顺序链**——①**保洁等无争议类**：处理人完成即结（`processing` → `closed`）；②**投诉类**：须**提单方确认**（`processing` → `pending_review` → `closed`），客人端投诉的提单方即客人；③**复杂类**（大型设备报修等）：须**主管验收 ＋ 提单方确认双闸**（`processing` → `pending_acceptance` → `accepted` → `pending_review` → `closed`），`accepted` 在复杂类分支下**可达**。原「待回访 → 待验收全类顺序链」读法作废。**`acceptance_by` 扩双闸取值与「复杂类」判定口径列入排期**，见 §6 待建一览 B 项 |
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
| 合同 `contracts` | `energy_fee_basis` | `none` / `by_contract` / `by_area` / `by_usage`（V162 当前 4 值；`EnergyService` 当前仍接受 `by_area`/`by_usage` 并据 `public_area` 用量计算商户应收） | **业务目标**：`public_area` 只作公区成本归集/分析，不向商户分摊；公区不形成商户能源费应收。待实现：写入侧拒绝 `by_area`/`by_usage`、结算侧停止公区应收，并在研发环境直接把 CHECK 收敛为 `none`/`by_contract`；本轮不改迁移或代码。 |

> **工单分表口径（2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26 定稿，见 §0）**：上表的 `facility_work_orders`（V166）只是**设施工单**主表；**商户服务工单彻底分表**，主写 `merchant_service_work_orders`（V022），采用 ENUM「商户服务工单状态」条的**独立七态状态链**（已提交/已派单/处理中/待商户确认/已处理待关闭/已关闭/已取消），并按 ENUM「工单时效」在看板上**分行对照**展示。上表 `facility_work_orders.status` 九态**不适用于商户服务工单**。原 F2 批「统一主表、`source=merchant_self` 同表承接」口径已撤销。

## 2. 设施设备台账（V165）
**业务规则**
- 建台账时记录名称、4 分类、安装空间、品牌、型号、供应/厂商、责任人、项目/商户归属；初始状态固定为 `normal`。
- **设备维保类型业务目标**：逐字采用 ENUM「设备维保类型」的**定期保养、例行巡检、故障维修、安全检测、部件更换**。当前 V165 与 `FacilityEquipmentRequest` 均没有维保类型字段/约束，也没有维保记录与设施工单的关联，列入 §6 G 待建。
- 每台设备单独配置 `maintenance_cycle_days`。下次维保到期 `next_maintenance_due` 由 `install_date` 或最近一次 `last_maintenance_at` 加周期派生并固化；`GET /maintenance-due` 读取到期清单。
- **到期维保单业务目标**：到期提醒自动生成维保单。当前 `FacilityEquipmentService` 只提供到期清单查询和 `maintenance-done` 回写，未见到期扫描/自动创建设施工单的服务或定时任务，列入 §6 I 待建。
- 状态改为 `alarm` 时必须带 `alarm_source=manual|iot`；离开 `alarm` 会清空告警源、备注和告警时间。
- 状态改为 `alarm` 或 `fault` 时发布 `FacilityEquipmentAlarmedEvent`，工单服务监听后自动生成 `repair/high` 工单，`source=equipment_alarm`，标题为“设备告警: 设备名”。
- 维保完成调用 `maintenance-done`：写 `last_maintenance_at=now`，状态回 `normal`，按维保周期重算 `next_maintenance_due`。
- **设备报废业务目标**：由所有权主体确认并按原审批办理，记录批准人、处置方式与实际处置凭证，残值或损失由财务关联原设备记录。报废状态不表示实物已处置或款项已结清；借入设备仍须双方确认，实物与赔偿款分别收尾。

**关键端点（实际 Controller）**
- `GET /api/v1/facility/equipment`
- `GET /api/v1/facility/equipment/maintenance-due`
- `POST /api/v1/facility/equipment`
- `POST /api/v1/facility/equipment/{equipment_id}`
- `POST /api/v1/facility/equipment/{equipment_id}/status`
- `POST /api/v1/facility/equipment/{equipment_id}/maintenance-done?idempotency_key=...`

**迁移 / 表 / 权限**
- 迁移：`V165__facility_equipment.sql`
- 表：`facility_equipment`
- 权限：`facility.equipment.manage`（`domain=platform`、`risk_level=medium`、`requires_approval=false`）

## 3. 设施工单（V166）
**业务规则**
- **自动告警接警与休息边界**：当班且承担接警任务的人员主动确认“已收到并接手”，送达、响铃或接听均不等于确认；未确认按已配置告警类型、优先级和当班名单升级，自动语音仅催接正在当班且承担该任务的人。设备负责人休息时只收系统消息，下一工作时段确认，休息期间不计响应超时、不电话催促。设备停用或继续使用按专业负责人及维保单位预先确认的故障规则处理，未覆盖由当班专业人员判定；不等于授权平台直接远程停机。涉及人身安全立即按应急流程处置，不等待负责人上班或系统确认。接警后继续派修和跟踪，换班未结事项由下一班明确接手。
- **系统事件提单确认**：设备或点位在岗责任人承接确认；责任人不在岗或本人负责此次维修，由物业主管指定其他人员并留痕。复杂工单仍须主管／物业验收与提单责任人确认两项通过才关闭；接警不等于维修完成。
- 当前设施工单类型为 6 类、优先级 3 类；内部协作工单是 §1 所列业务目标及待建缺口。SLA 不硬编码在服务里，按 `facility_work_order_policy(work_order_type, priority)` 查响应时限和处理时限，唯一约束 `uq_fwop_type_priority`。
- 默认策略（**2026-08-04 对齐 BSM「运营细则（已确认）」条**）：保洁/巡检/其他按**处理人完成即结**（无争议类）；投诉按**提单方确认**（一律须提单方确认才能关闭）；**报修/整改按主管验收**，其中**复杂类（大型设备报修）须「主管/物业验收**并且**提单方确认」双闸——两者都通过才能关闭，缺任一不得关闭**。后台可通过 `POST /policies` 调整。
  - **（实现待对齐）** 当前 `acceptance_by` 为 `handler`/`supervisor`/`reporter` **三选一**，表达不了「主管验收 + 提单方确认」的与关系；落地二选一：**扩展枚举加 `supervisor_and_reporter`**，或**在 `facility_work_order_policy` 加独立标志位 `require_reporter_confirm`（与 `acceptance_by` 正交）**。本轮**不改 DB CHECK、不改服务分流**，只把口径与缺口写实（见 §1 `acceptance_by` 行、§6 待建一览）；「复杂类」的判定口径（按工单类型 × 设备大类，还是按策略行显式勾选）随该项一并定案。
- 创建工单时根据策略写 `response_due_at`、`handle_due_at`；当前 `mark-overdue` 仅扫描超期未关闭设施工单并置 `overtime=true`。**业务目标**是按响应/处理时限超时后自动通知并升级主管；当前缺通知、主管接收人解析、升级留痕和设施工单定时触发，列入 §6 H 待建。
- 设施工单的 `source` 当前仍允许 `merchant_self`；但现行业务裁决是商户服务工单由 V022 独立表承接，设施工单与商户服务工单彻底分表、各自状态链。故 `merchant_self` 在 V166 的当前允许值是待对齐缺口，不把它作为同表承接的业务口径；整改与巡检仍由平台、设备告警或巡检异常发起。
- 主流程：`pending_assign` → `assigned` → `processing`。完成时按 `acceptance_by` 分流：`handler` 直接 `closed`，`supervisor` 进 `pending_acceptance`，`reporter` 进 `pending_review`。
- `accept` / `review` 可记录 `satisfaction` 1-5，关闭时写 `verified_at`、`verifier_id`，并对关联 `equipment_id` 的设备回写 `normal`。
- 支持 `suspend` / `resume`；只有活动态可挂起，挂起后恢复为 `processing`。
- 事件解耦：设备 `alarm/fault` 自动转 `repair/high`；巡检异常 `to_work_order` 自动转 `rectification/normal` 并把 `work_order_id` 回写巡检任务。

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

**关键端点（实际 Controller）**
- `GET /api/v1/facility/work-orders`
- `GET /api/v1/facility/work-orders/overdue`
- `GET /api/v1/facility/work-orders/{work_order_id}`
- `POST /api/v1/facility/work-orders`
- `POST /api/v1/facility/work-orders/{work_order_id}/assign`
- `POST /api/v1/facility/work-orders/{work_order_id}/start`
- `POST /api/v1/facility/work-orders/{work_order_id}/complete`
- `POST /api/v1/facility/work-orders/{work_order_id}/accept`
- `POST /api/v1/facility/work-orders/{work_order_id}/review`
- `POST /api/v1/facility/work-orders/{work_order_id}/suspend`
- `POST /api/v1/facility/work-orders/{work_order_id}/resume`
- `POST /api/v1/facility/work-orders/mark-overdue`
- `POST /api/v1/facility/work-orders/policies`

**迁移 / 表 / 权限**
- 迁移：`V166__facility_work_orders.sql`
- 表：`facility_work_order_policy`、`facility_work_orders`
- **商户服务工单表（分表，2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26）**：`merchant_service_work_orders` / `merchant_service_work_order_actions`（V022）是**商户服务工单的主写表**，与设施工单彻底分表、各自状态链；商户专区「我的工单」ADM-MER-004 继续走 `/api/v1/merchant-service/work-orders`（`MerchantServiceController` → V022），**不向 `/api/v1/facility/work-orders` 收敛**。V022 的 7 值 CHECK 已存在；应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`），`MerchantServiceService` 已实现创建、派单、处理、待商户确认、已处理待关闭、关闭和逾期打标/动作留痕；`cancelled` 只被识别为终态，无取消入口、当前不可达。仍待补齐 `cancelled` 的合法取消入口，并对齐 V166 现仍接受 `merchant_self`、其历史迁移注释还称 V022 停止写入、设施/商户工单看板分行对照及商户工单不显示首次响应指标。本轮只改 PRD 口径，**不改 CSV、不改代码、不改迁移**。
- 权限：`facility.work_order.manage`（`domain=platform`、`risk_level=medium`、`requires_approval=false`）

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

## 5. 能耗抄表（V168）
**业务规则**
- 表计 4 类，支持 `parent_meter_id` 总分表层级；`GET /meters/{meter_id}/difference?period=YYYY-MM` 用总表用量减分表用量做抹差核对。
- 归集对象 3 类：`public_area`、`merchant`、`equipment_circuit`，分别用于**公共区域成本归集/分析**、商户直读、设备/回路分析；`public_area` 不对商户分摊。
- **抄表主次业务目标**：远程表自动读为主，人工抄表只作异常或补充路径。V168 的 `read_mode` 默认 `remote`，当前 `EnergyService` / `EnergyReadingRecordRequest` 均仍接受 `remote` 与 `manual`；未在服务层限制人工仅作异常/补充，列入 §6 J 待建。
- 抄表状态落 4 态。当前服务按同表计上一期自动取 `prev_reading_value`，不信任请求体上期值，并把首期上期值设为0；该实现不能直接用于换表。业务目标：换表记录旧表终值、新表初值、换表时点及原计费对象，两段按各自实际计量计算，不跨表作差。新表起点采用经核实初值，不能因首期默认为0多计；无法核实走原异常复核，核实前暂停出账，不引入估抄或新收费。
- 异常识别：读数倒走为 `reverse`；读数或本期消耗为 0 为 `zero`；上一期消耗大于 0 时，按环比百分比与 `energy_anomaly_thresholds` 比较，超过 `surge_pct` / `drop_pct` 分别记 `surge` / `drop`。
- `zero`、`reverse`、`surge`、`drop` 均进入 `status=abnormal`，通过 `review` 复核为 `reviewed`；异常不是拒收（读数照收、进复核，不丢数据）。
- **异常读数处置业务目标（2026-08-04 对齐 BSM「能耗抄表」条／PGM「表计按总表-分表分层、可总分表抹差核对」条）**：**判为异常（突增/突降/零读数/读数倒走）的读数一律不得直接出账**；须由**第二人复抄或到场核表**，核实后**填写原因并经主管确认**才转为 `reviewed`（已审核）。**核实完成之前，该商户当期能源费账单暂缓生成、暂缓催缴**；核实须**在限期内出结果、不得无限期暂缓**（业务参数，按项目可配，出厂默认一周，须同时配置上下限）。**异常读数被更正的，保留原始录入值与更正记录，两者都可查**。**每次抄录必须同时上传表底照片，系统自动与上期读数比对**（上期值由服务按同表计上一期自动取，不信任请求体，见上）。**当前实现缺口**：V168 / `EnergyReadingRecordRequest` 无表底照片、复抄人、到场核表、核实原因、主管确认、复核限期或更正记录字段；`EnergyReadingReviewRequest` 只有 `reviewedBy`，`EnergyService.reviewReading` 可直接将读数写为 `reviewed`；结算未按异常核实状态暂缓，列入 §6 K 待建。
- 异常阈值表 `energy_anomaly_thresholds` 按 `meter_type` 配置，V168 默认四类各 50%；当前未暴露独立阈值 REST 端点。

**能源费业务口径（2026-08-04 对齐 PGM「表计按总表-分表分层、可总分表抹差核对」条／BSM「能耗抄表」条／ENUM「能源费与损耗费」条＋ENUM「物业费」条）**
- **商户能源费＝代收、不得加价**：商户能源费**按其独立表计的实际用量与实际费用代收，一律不得加价**；该笔款项在平台中记为**代收代缴往来，不作为城市项目公司的经营收入**（不计入物业费收入或普通经营收入）。
- **公区能耗不向商户分摊**：公区能耗**仍按公共区域归集，只做成本分析与能耗管理，但不形成对商户的公区能源费应收**；公区公摊费用**统一含在物业费中**，**不再生成公区能源费账单**。
- **电损、水损归入物业费**：总表与分表差额仅用于核对和成本分析；电损、水损等费用按已批准物业费合同及规则承接，不另向商户收电损费、水损费或独立服务费，不另开该名目的服务发票。商户能源费仍按独立表计实际用量和实际费用代收，不能通过改名加价，公区能源也不另行分摊。
- **实现待对齐**：删除“新建独立电损／水损计费”的目标，既有实现事实不作为收费授权；物业费与能源往来分别登记，复核同期间不重复收费。本轮仅改文档，不改代码、迁移或 OpenAPI。
- **`energy_fee_basis` 口径对齐（当前与目标分列）**：`contracts.energy_fee_basis` 的当前 4 值 CHECK（`none`/`by_contract`/`by_area`/`by_usage`）来自 V162；当前 `EnergyService` 仍允许 `by_area`/`by_usage`，并以 `public_area` 用量计算商户应收。**业务目标**：`none` 不结算；`by_contract` 取合同 `rent_terms.energyFeeCent`；`by_area`（按商户面积分摊公区能耗费）与 `by_usage`（按商户用量比例分摊公区能耗费）均已过期作废，公区能耗只作归集与成本分析、不向商户分摊、不形成商户应收；商户能源费走独立表计实际用量实际费用代收，电损、水损归入物业费，不另形成能源差额收费。**待实现**：研发环境无生产存量数据、测试数据可直接清理，故需将 CHECK 收敛为 `none`/`by_contract` 两值，且同步拒绝新写入与停止结算侧公区应收；不单独立项做历史迁移／回填。本轮不改迁移或代码。
- 结算写 `ar_ap_items`：`direction=receivable`、`source_type=energy_fee`、`source_id=energy_fee_{merchant|period stable token}`、`status=pending_collection`，并写 `ar_ap_item_sources`；同商户同期间已存在则跳过，保证幂等。

**关键端点（实际 Controller）**
- `GET /api/v1/energy/meters`
- `POST /api/v1/energy/meters`
- `POST /api/v1/energy/meters/{meter_id}`
- `GET /api/v1/energy/readings?period=YYYY-MM`
- `POST /api/v1/energy/readings`
- `POST /api/v1/energy/readings/{reading_id}/review`
- `GET /api/v1/energy/meters/{meter_id}/difference?period=YYYY-MM`
- `POST /api/v1/energy/settlements/energy-fee`
- `POST /api/v1/energy/settle-energy-fee`（兼容别名）

**迁移 / 表 / 权限**
- 迁移：`V168__energy_metering.sql`；能源费分摊合同 CHECK 来自 `V162__contract_lifecycle.sql`
- 表：`energy_meters`、`energy_meter_readings`、`energy_anomaly_thresholds`、`ar_ap_items`、`ar_ap_item_sources`
- 权限：`energy.meter.manage`（medium、无需审批）、`energy.reading.manage`（high、`requires_approval=true`）

## 5A. 施工完工、整改与验收（HP-F04）

- 供应商只可对指派给本人、处于施工中或整改中的报备上传真实文件并提交完工材料；未提供材料不能申请验收。整改重提须包含本轮新增材料，原材料与验收意见保留。
- 每次完工提交、验收通过或要求整改，分别记录操作者、时间、材料、意见及顺序号。验收引用当前所见的完工提交序号；没有待验收提交、引用旧序号或重复处置均拒绝，重复请求返回首次结果，不重复追加历史。
- 物业人员在“进场审批”与“施工验收”入口分别按对应权限及项目范围办理；供应商只能看本人报备。材料下载核对原报备、真实文件与操作者范围，不公开文件路径。
- `facility_site_entry_history` 由 V467 建立，只追加不可回改。报备状态更新与历史追加在同一事务，并锁定报备以避免整改、重新提交和验收互相覆盖。
- 端点沿用 `/api/v1/facility/site-entry-reports/{report_id}` 的提交和验收，新增 `/history`、真实文件 `/attachments` 与 `/attachments/{attachment_id}/file`；验收请求增加必填 `completion_sequence`。页面为 ADM-FAC-002、ADM-FAC-003 及供应商原进场报备页。

## 6. 待建一览（M08 现状登记）

> 口径已按 HTML 写实，落地未做；本表所列待建项本轮不改代码、迁移、OpenAPI；CSV 与业务 HTML 仅限 James V0.06 已批准的 PGM H1–H3 及入口注册表、规则账本、覆盖矩阵机械镜像。

| 编号 | 待建/待对齐项 | 现状 | 需要的动作 | 依据 |
|---|---|---|---|---|
| A | **电损、水损归入物业费** | 原独立服务费方案已被Q21替代；结算实现须另行核对 | 按批准物业费合同及规则承接，不新建独立电损／水损收费或服务发票；能源代收与物业费分开且不重复收费（见§5） | Q21；现行能源与物业费 HTML 条款 |
| B | **复杂类工单「主管验收 + 提单方确认」双闸** | `acceptance_by` 三选一，表达不了与关系 | 枚举扩 `supervisor_and_reporter` 或加独立标志位 `require_reporter_confirm`，并定「复杂类」判定口径（见 §1/§3） | BSM「运营细则（已确认）」条 |
| C | **商户服务工单分表独立七态**（2026-08-04 裁决改向：原「收敛到 V166」已撤销） | V022 的 7 值 CHECK 已存在；应用可达状态仅六个（`created`/`assigned`/`processing`/`pending_merchant`/`resolved`/`closed`）；`cancelled` 只被识别为终态，无取消入口、当前不可达 | 补齐 `cancelled` 的合法取消入口；对齐 V166 `merchant_self` 当前仍可写入、V166 历史注释称 V022 停止写入，以及设施/商户看板分行对照与商户单不显示首次响应指标（见 §3） | M08-CX-019/020；ENUM「商户服务工单状态」「工单时效」条 |
| D | **`energy_fee_basis` 收敛迁移** | 当前 DB CHECK 仍 4 值，`EnergyService` 仍允许 `by_area`/`by_usage` 并按 `public_area` 生成商户应收 | 直接收敛 DB CHECK 为 `none`/`by_contract` 两值，并在写入和结算侧禁用公区对商户分摊/应收（研发环境无生产存量数据，测试数据直接清理，不做历史迁移／回填立项） | M08-CX-021/022/023；GROK-M08-033/037/038；第三轮用户裁决 UD-07 / DEC-R6-28 |
| E | **工单来源补 `customer`** | `facility_work_orders.source` CHECK 现 4 值、无 `customer` | CHECK 扩 `customer`；客人端投诉直接落工单、客人为提单方、关闭须其确认（见 §1 `source` 行） | 第三轮用户裁决 UD-03 / DEC-R6-14；BSM 物业服务章·主要功能 |
| F | **工单九态分支语义落地** | `acceptance_by` 三选一，表达不了「主管验收 ＋ 提单方确认」双闸；`accepted` 现不可达 | 按 UD-05 / DEC-R6-13 三类分支实现：无争议类完成即结、投诉类提单方确认、复杂类双闸；并定「复杂类」判定口径（与本表 B 项同一改造） | 第三轮用户裁决 UD-05 / DEC-R6-13；ENUM「物业工单状态」条 |
| G | **设备维保五类及记录承载** | V165 / `FacilityEquipmentRequest` 无 `maintenance_type` 字段、CHECK、维保记录或工单关联 | 按 ENUM 逐字补定期保养、例行巡检、故障维修、安全检测、部件更换的字段、值域与维保记录/工单关联 | GROK-M08-028；ENUM「设备维保类型」条 |
| H | **设施工单超时通知与升级主管** | `FacilityWorkOrderService.markOverdue` 仅置 `overtime=true`，无设施工单定时触发、通知、主管解析或升级留痕 | 补按响应/处理时限自动通知并升级主管的定时/服务/留痕链路 | GROK-M08-029；BSM「按响应时限和处理时限统计时效，超时自动升级通知主管」条 |
| I | **设备到期自动生成维保单** | `FacilityEquipmentService` 仅有 `maintenance-due` 查询与 `maintenance-done` 回写，无到期扫描/自动创建设施工单 | 补到期扫描、按设备生成维保单及幂等/留痕链路 | GROK-M08-030；PGM「设备分电梯扶梯、暖通空调、配电供电、给排水消防四组台账」条 |
| J | **远程抄表主路径与公区归集** | V168 默认 `remote` 但服务仍接受任意 `manual`；`EnergyService` 仍把 `public_area` 用于 `by_area`/`by_usage` 商户应收 | 限人工为异常/补充路径；`public_area` 只作成本归集/分析，停止公区对商户分摊和应收 | GROK-M08-034；M08-CX-021/022/023；PGM「表计按总表-分表分层、可总分表抹差核对」条 |
| K | **异常抄表复核闸门** | V168 / `EnergyReadingRecordRequest` 无照片、复抄/到场、原因、主管确认、限期、更正记录字段；`EnergyReadingReviewRequest` 仅 `reviewedBy`，服务可直接写 `reviewed`，结算未按核实状态暂缓 | 补表底照片、第二人复抄/到场核表、原因、主管确认、限期复核、更正留痕及异常账单暂缓 | M08-CX-024；GROK-M08-039/040；BSM「能耗抄表」条 |
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
