# 楼宇运营业务域 PRD（V1.0·据需求重建）

> 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。

> 生成：2026-06-27。依据：M1 需求采集 §3-§6 + 业务 HTML §十（地产运营：设备/物业工单/能耗/巡检枚举）+ `platform-governance-map-for-operations-v0.1.html`「楼宇运营」+ `business-system-map-for-operations-v0.1.html`「物业服务/楼宇运营」+ V165-V168 迁移 + `com.complex.platform.facility` / `com.complex.platform.energy` Controller 与 Service。本文向业务 HTML §十对齐；冲突以业务 HTML 为最终解释权，本文同时逐字登记本轮真实 DB CHECK 与实际端点。
> 重建动因：据需求重建，旧骨架枚举漂移(设备3→6态/工单类型3→6/表计3→4/巡检4技术态→7业务态)已纠正。楼宇本体运营不能继续混在员工工作设备、游乐设备或旧商户服务工单里；V165-V168 已把楼宇机电设备、维保/物业工单、楼宇巡检、能耗抄表拆成独立闭环，本文补齐产品/技术基线，避免后续再用旧 V022 工单或游乐安全巡检替代楼宇域。

## 0. 定位与边界
- 楼宇运营 = 综合体建筑本身的机电设备台账、维保/物业工单、楼宇巡检、能耗抄表与能源费应收。
- 数据隔离口径 = 综合体/项目级。表上保留 `project_id`，多数写路径在 Service 层调用 `authorizationService.requirePlatformScope()`，商户侧自助提单通过 `source=merchant_self` 进入同一工单表。
- 楼宇设备不同于员工工作设备、游乐设备：本域只管电梯/暖通/配电/给排水消防等建筑机电设施。
- **工单主表口径（2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26 定稿：彻底分表）**：**设施工单与商户服务工单分属两张表、各自独立状态链，不并主表**——
  - **设施工单**走 V166 `facility_work_orders`，承接设备告警、巡检异常、平台发起与现场工作人员报修，状态链见 §1 `status` 行（九态）；
  - **商户服务工单**走 `merchant_service_work_orders` / `merchant_service_work_order_actions`（V022），**恢复为该域主写表**，采用 ENUM「商户服务工单状态」条的**独立七态状态链**：已提交 / 已派单 / 处理中 / 待商户确认 / 已处理待关闭 / 已关闭 / 已取消（DB 侧为独立 ENUM／CHECK，不与设施工单九态混用）；
  - 看板按 ENUM「工单看板分行对照」要求**分行对照展示**，两类工单不合并计数、不互相映射状态。
  - **本条撤销 2026-08-04 F2 批的「统一主表、商户服务工单以 `source=merchant_self` 同表承接」口径**（该口径当时为待裁决登记项，现按用户裁决作废）。商户专区「我的工单」ADM-MER-004 继续走 `/api/v1/merchant-service/work-orders`，**无需向 `/api/v1/facility/work-orders` 收敛**；分表改造（补齐商户侧独立七态 CHECK 与状态机、页面权限字段矩阵同步）**列入排期**。同一口径见 `real-estate-full-chain-domain-prd-v0.1.md` §3、`merchant-portal-zone-prd-v0.1.md` §3/§6。
- 能耗抄表独立归 `energy` 域；**（2026-08-04 第三轮用户裁决 UD-04 / DEC-R6-12 定稿）** 楼宇巡检类型**四类**——安防、消防、设施设备、环境卫生，**删除「能耗抄表」**，能耗抄表一律走**能耗域独立抄表流程**（按 PGM「巡检运营细则」口径收口）。ENUM「楼宇巡检类型」条已同批改为四类，原五类口径作废。**DB CHECK 现为 4 值**（`security`/`fire`/`facility`/`environment`），与本裁决一致，**无需改造**。
- 不做：具体 IoT 协议接入、第三方远程表厂商对接、备件库存采购闭环；这些只保留字段或上下游连接点。

## 1. 业务对象与状态枚举（对齐 HTML §十，逐字落 DB CHECK）
| 对象 | 枚举/字段 | 取值（DB CHECK / 服务校验原值） | 业务说明 |
|---|---|---|---|
| 设施设备 `facility_equipment` | `equipment_type` | `elevator` / `hvac` / `power` / `water_fire` | 电梯扶梯 / 暖通空调 / 配电供电 / 给排水消防 |
| 设施设备 `facility_equipment` | `status` | `normal` / `alarm` / `fault` / `maintenance` / `disabled` / `retired` | 正常 / 告警 / 故障 / 维保中 / 停用 / 报废 |
| 设施设备 `facility_equipment` | `alarm_source` | `manual` / `iot` | 人工上报 / 远程或物联自动采集；仅告警时填写 |
| 工单策略 `facility_work_order_policy`、工单 `facility_work_orders` | `work_order_type` | `repair` / `cleaning` / `complaint` / `rectification` / `inspection` / `other` | 报修 / 保洁 / 投诉 / 整改 / 巡检 / 其他服务请求 |
| 工单策略/工单 | `priority` | `urgent` / `high` / `normal` | 用于匹配 SLA「类型 × 优先级」 |
| 工单策略 | `acceptance_by` | `handler` / `supervisor` / `reporter` | 处理人完成即结 / 主管验收 / 提单方确认。**（2026-08-04 口径，对齐 BSM L1235「运营细则（已确认）」）复杂类（大型设备报修）须「主管/物业验收**并且**提单方确认」双闸**——当前 3 值枚举**表达不了该组合，实现待对齐**（二选一：枚举扩展一个 `supervisor_and_reporter` 值，或在策略表加独立标志位 `require_reporter_confirm`）；本轮**不改 DB CHECK**，见 §3 默认策略与 §6 待建一览 |
| 工单 `facility_work_orders` | `source` | `merchant_self` / `customer` / `platform` / `equipment_alarm` / `inspection` | 商户自助 / **客人（客人端投诉）** / 平台发起（含现场工作人员） / 设备告警 / 巡检异常。**（2026-08-04 第三轮用户裁决 UD-03 / DEC-R6-14 定稿）** 工单来源**补 `customer`**：客人投诉由客人自行在客人端提交，**客人即为提单方，关闭须其确认**（与 §1 `status` 行的分支语义、BSM 物业服务章·主要功能一致）。**DB CHECK 扩值列入排期**（现为 4 值、无 `customer`），属待改造缺口，见 §6 待建一览 |
| 工单 `facility_work_orders` | `status` | `pending_assign` / `assigned` / `processing` / `pending_review` / `pending_acceptance` / `accepted` / `closed` / `suspended` / `overtime` | 待派单 / 已派单 / 处理中 / 待回访 / 待验收 / 已验收 / 已关闭 / 已挂起 / 已超时；`accepted` 为 DB 保留值，当前服务验收后直接写 `closed`。**（2026-08-04 第三轮用户裁决 UD-05 / DEC-R6-13 定稿）** 九态**按工单类型分支适用，不是全类顺序链**——①**保洁等无争议类**：处理人完成即结（`processing` → `closed`）；②**投诉类**：须**提单方确认**（`processing` → `pending_review` → `closed`），客人端投诉的提单方即客人；③**复杂类**（大型设备报修等）：须**主管验收 ＋ 提单方确认双闸**（`processing` → `pending_acceptance` → `accepted` → `pending_review` → `closed`），`accepted` 在复杂类分支下**可达**。原「待回访 → 待验收全类顺序链」读法作废。**`acceptance_by` 扩双闸取值与「复杂类」判定口径列入排期**，见 §6 待建一览 B 项 |
| 巡检模板/任务 | `inspection_type` | `security` / `fire` / `facility` / `environment` | 安防 / 消防 / 设施设备 / 环境卫生；**能耗抄表不属巡检类型**，走能耗域独立抄表流程（2026-08-04 第三轮用户裁决 UD-04 / DEC-R6-12 定稿为四类，ENUM「楼宇巡检类型」条已同批改为四类，见 §0）。DB CHECK 4 值与裁决一致，无需改造 |
| 巡检模板 | `abnormal_mode` | `light` / `to_work_order` | 轻记录闭环 / 转整改工单闭环 |
| 巡检任务 `facility_inspection_tasks` | `status` | `pending` / `in_progress` / `normal` / `abnormal` / `rectifying` / `rechecked` / `closed` | 待执行 / 执行中 / 正常 / 发现异常 / 整改中 / 已复检 / 已关闭 |
| 巡检任务 | `schedule_mode` | `template` / `manual` / `route` | 模板自动排 / 主管手动排 / 点位路线排 |
| 巡检记录 `facility_inspection_records` | `result` | `normal` / `abnormal` | 正常 / 异常 |
| 能耗表计 `energy_meters` | `meter_type` | `electricity` / `water` / `gas` / `heat_cold` | 电表 / 水表 / 燃气表 / 冷热量表 |
| 能耗表计 `energy_meters` | `gather_object` | `public_area` / `merchant` / `equipment_circuit` | 公共区域 / 商户 / 设备或回路 |
| 能耗表计/读数 | `read_mode` | `remote` / `manual` | 远程自动读 / 人工抄表 |
| 抄表读数 `energy_meter_readings` | `status` | `pending` / `recorded` / `reviewed` / `abnormal` | 待抄 / 已抄录 / 已审核 / 异常 |
| 抄表读数 `energy_meter_readings` | `abnormal_reason` | `surge` / `drop` / `zero` / `reverse` | 突增 / 突降 / 零读数 / 读数倒走 |
| 合同 `contracts` | `energy_fee_basis` | `none` / `by_contract` / `by_area` / `by_usage` | 公区能源费不分摊 / 按合同约定 / 按面积分摊 / 按用量比例；CHECK 来自 V162，结算由 V168 能耗域读取。**（2026-08-04 口径）`by_area`/`by_usage` 的「公区能耗向商户分摊」含义已过期作废——公区能耗不向商户分摊，见 §5「能源费业务口径」（ENUM L131「能源费与损耗费」／ENUM L130「物业费」／PGM L760「能耗运营细则（已确认）」）；DB CHECK 4 值本次不改。**硬约束：收敛迁移完成前，新签与变更合同一律禁止新写入 `by_area`/`by_usage`**（只允许 `none`/`by_contract`；写入侧校验拒绝，不做静默改写）。**存量处置已简化（2026-08-04 第三轮用户裁决 UD-07 / DEC-R6-28）**：当前处于研发阶段、**无生产存量数据、测试数据可直接清理**，因此**直接收敛 DB 取值**——CHECK 收敛为 `none`/`by_contract` 两值即可，**不为历史数据迁移／回填单独立项**。原「存量合同处置待裁决」标注已删除 |

> **工单分表口径（2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26 定稿，见 §0）**：上表的 `facility_work_orders`（V166）只是**设施工单**主表；**商户服务工单彻底分表**，主写 `merchant_service_work_orders`（V022），采用 ENUM「商户服务工单状态」条的**独立七态状态链**（已提交/已派单/处理中/待商户确认/已处理待关闭/已关闭/已取消），并按 ENUM「工单看板分行对照」在看板上**分行对照**展示。上表 `facility_work_orders.status` 九态**不适用于商户服务工单**。原 F2 批「统一主表、`source=merchant_self` 同表承接」口径已撤销。

## 2. 设施设备台账（V165）
**业务规则**
- 建台账时记录名称、4 分类、安装空间、品牌、型号、供应/厂商、责任人、项目/商户归属；初始状态固定为 `normal`。
- 每台设备单独配置 `maintenance_cycle_days`。下次维保到期 `next_maintenance_due` 由 `install_date` 或最近一次 `last_maintenance_at` 加周期派生并固化；`GET /maintenance-due` 读取到期清单。
- 状态改为 `alarm` 时必须带 `alarm_source=manual|iot`；离开 `alarm` 会清空告警源、备注和告警时间。
- 状态改为 `alarm` 或 `fault` 时发布 `FacilityEquipmentAlarmedEvent`，工单服务监听后自动生成 `repair/high` 工单，`source=equipment_alarm`，标题为“设备告警: 设备名”。
- 维保完成调用 `maintenance-done`：写 `last_maintenance_at=now`，状态回 `normal`，按维保周期重算 `next_maintenance_due`。

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

## 3. 维保工单（V166）
**业务规则**
- 工单类型 6 类、优先级 3 类；SLA 不硬编码在服务里，按 `facility_work_order_policy(work_order_type, priority)` 查响应时限和处理时限，唯一约束 `uq_fwop_type_priority`。
- 默认策略（**2026-08-04 对齐 BSM L1235「运营细则（已确认）」**）：保洁/巡检/其他按**处理人完成即结**（无争议类）；投诉按**提单方确认**（一律须提单方确认才能关闭）；**报修/整改按主管验收**，其中**复杂类（大型设备报修）须「主管/物业验收**并且**提单方确认」双闸——两者都通过才能关闭，缺任一不得关闭**。后台可通过 `POST /policies` 调整。
  - **（实现待对齐）** 当前 `acceptance_by` 为 `handler`/`supervisor`/`reporter` **三选一**，表达不了「主管验收 + 提单方确认」的与关系；落地二选一：**扩展枚举加 `supervisor_and_reporter`**，或**在 `facility_work_order_policy` 加独立标志位 `require_reporter_confirm`（与 `acceptance_by` 正交）**。本轮**不改 DB CHECK、不改服务分流**，只把口径与缺口写实（见 §1 `acceptance_by` 行、§6 待建一览）；「复杂类」的判定口径（按工单类型 × 设备大类，还是按策略行显式勾选）随该项一并定案。
- 创建工单时根据策略写 `response_due_at`、`handle_due_at`；`mark-overdue` 扫描超期未关闭工单并置 `overtime=true`。
- 商户自助提单只允许 `repair`、`cleaning`、`complaint`、`other`；`rectification` 与 `inspection` 由平台、设备告警或巡检异常发起。
- 主流程：`pending_assign` → `assigned` → `processing`。完成时按 `acceptance_by` 分流：`handler` 直接 `closed`，`supervisor` 进 `pending_acceptance`，`reporter` 进 `pending_review`。
- `accept` / `review` 可记录 `satisfaction` 1-5，关闭时写 `verified_at`、`verifier_id`，并对关联 `equipment_id` 的设备回写 `normal`。
- 支持 `suspend` / `resume`；只有活动态可挂起，挂起后恢复为 `processing`。
- 事件解耦：设备 `alarm/fault` 自动转 `repair/high`；巡检异常 `to_work_order` 自动转 `rectification/normal` 并把 `work_order_id` 回写巡检任务。

**工单用料与费用承担方（2026-08-04 对齐 BSM L1230「派单到具体人员或班组」）**
- 工单**用料必须从指定库房出库**，并**按当前平均进货价计成本**（平均进货价口径：每次新进货后把新到的货与原有库存合并算出新的平均单价，出库即按该均价计成本）。
- 工单上**必须标明费用承担方**：**公区设施与楼宇主体设备**的用料**由物业承担**；**商户自有设施、以及因商户责任导致损坏**的用料**由该商户承担，并自动生成一笔应收进入其费用账单**。
- **承担方有争议的，工单挂起转上级判定**（复用既有 `suspend`/`resume`），**判定结果与依据留痕后才能结案**。

**客诉闭环机制（2026-08-04 对齐 BSM L1235「运营细则（已确认）」）**
- **工单由客户关怀部负责处理**；**响应时限与处理时限对商户公开**（即 `facility_work_order_policy` 的「类型 × 优先级」时限对商户侧可见）。
- **投诉类一律须提单方确认才能关闭**；提单方在后台配置的时限内未确认的**视为默认关闭**（业务参数，按工单类型可配），但**单独统计为「未确认关闭」**，不与正常确认关闭混计。
- **所有客诉必须有跟进、有记录、有结果，直至形成闭环**；**客诉须经客户关怀部确认后方可视为形成闭环**，未经其确认的一律不算闭环。
- **系统每周自动生成客诉周报**，列明当周客诉的**笔数、类型、处理进度与闭环情况**——**店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人**。
- **升级铁律**：**一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人**；**是否属于同一类由客户关怀部认定**（人工认定，系统只负责识别候选与推送）。

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
- **商户服务工单表（分表，2026-08-04 第三轮用户裁决 UD-06 / DEC-R6-26）**：`merchant_service_work_orders` / `merchant_service_work_order_actions`（V022）为**商户服务工单的主写表**，独立七态状态链；商户专区「我的工单」ADM-MER-004 继续走 `/api/v1/merchant-service/work-orders`（`MerchantServiceController` → V022），**不向 `/api/v1/facility/work-orders` 收敛**。本表**只承接商户服务工单**，不承接设施工单。**改造列入排期**：补齐商户侧独立七态 DB CHECK 与状态机（现取值与 ENUM 七态未逐值核对）、`docs/full-admin-page-permission-field-matrix-v0.1.csv` 的 ADM-MER-001／ADM-MER-004 端点与状态机列按分表口径同步——本轮只改 PRD 口径，**不改 CSV、不改代码、不改迁移**
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
- 归集对象 3 类：`public_area`、`merchant`、`equipment_circuit`，用于公共区域分摊、商户直读、设备/回路分析。
- 抄表状态落 4 态。录入读数时服务按同表计上一期自动取 `prev_reading_value`，不信任请求体里的上期值；首期上期值为 0。
- 异常识别：读数倒走为 `reverse`；读数或本期消耗为 0 为 `zero`；上一期消耗大于 0 时，按环比百分比与 `energy_anomaly_thresholds` 比较，超过 `surge_pct` / `drop_pct` 分别记 `surge` / `drop`。
- `zero`、`reverse`、`surge`、`drop` 均进入 `status=abnormal`，通过 `review` 复核为 `reviewed`；异常不是拒收（读数照收、进复核，不丢数据）。
- **异常读数处置（2026-08-04 对齐 BSM L1259「能耗抄表」／PGM L760「能耗运营细则（已确认）」）**：**判为异常（突增/突降/零读数/读数倒走）的读数一律不得直接出账**；须由**第二人复抄或到场核表**，核实后**填写原因并经主管确认**才转为 `reviewed`（已审核）。**核实完成之前，该商户当期能源费账单暂缓生成、暂缓催缴**；核实须**在限期内出结果、不得无限期暂缓**（业务参数，按项目可配，出厂默认一周，须同时配置上下限）。**异常读数被更正的，保留原始录入值与更正记录，两者都可查**。**每次抄录必须同时上传表底照片，系统自动与上期读数比对**（上期值由服务按同表计上一期自动取，不信任请求体，见上）。
- 异常阈值表 `energy_anomaly_thresholds` 按 `meter_type` 配置，V168 默认四类各 50%；当前未暴露独立阈值 REST 端点。

**能源费业务口径（2026-08-04 对齐 PGM L760「能耗运营细则（已确认）」／BSM L1259「能耗抄表」／ENUM L131「能源费与损耗费」＋ ENUM L130「物业费」）**
- **商户能源费＝代收、不得加价**：商户能源费**按其独立表计的实际用量与实际费用代收，一律不得加价**；该笔款项在平台中记为**代收代缴往来，不作为城市项目公司的经营收入**（不计入物业费收入或普通经营收入）。
- **公区能耗不向商户分摊**：公区能耗**仍按公共区域归集，只做成本分析与能耗管理，但不形成对商户的公区能源费应收**；公区公摊费用**统一含在物业费中**，**不再生成公区能源费账单**。
- **电损费与水损费（新增独立服务费项目）**：进楼总表用量 − 各商户分表用量之和 − 独立计量的设备回路用量，所形成的**差额（含线损、无表点位与表计误差）**，通过**电损费与水损费**按**各商户实际用电度数、用水吨数**另行**独立计收**，作为**独立服务费项目单列并单独开具服务发票**；按后台配置的**固定金额或比例**计收（业务参数，按项目可配，须同时配置上下限）。总分表差额的计算基础即现有 `GET /meters/{meter_id}/difference` 抹差核对能力。
- **电损费 / 水损费（待建·实现待对齐）**：口径已定（上条），**当前无任何落地**——`ar_ap_items.source_type` 无 `power_loss_fee`/`water_loss_fee`、无计费配置项、无独立发票标识、结算端点只出 `energy_fee`。实现须补齐：
  1. **计费对象**：按**商户**逐户计收（公区不计、不向商户分摊公区能耗）；参与分摊的分母为**各商户当期实际用电度数 / 实际用水吨数**（电损费按度、水损费按吨，两项**分别独立计算、分别计收**）。
  2. **公式**：差额 ＝ 进楼总表用量 − Σ 各商户分表用量 − 独立计量的设备回路用量；商户应分担额 ＝ 按其当期实际度数/吨数与后台配置的**固定金额**（元/度、元/吨）或**比例**（占其能源费或其用量的比例）计得，二选一由配置决定。
  3. **配置项**：计费方式（固定金额 / 比例）、单价或比例值、启用开关，均为**业务参数、按项目可配、须同时配置上下限**（超出上下限拒绝保存）。
  4. **台账与发票**：落 `ar_ap_items` 时使用**独立的 `source_type`（`power_loss_fee` / `water_loss_fee`，与 `energy_fee` 分开、不合并计入能源费）**，并带**独立发票标识**——作为**独立服务费项目单列、单独开具服务发票**；`source_id` 沿用「同商户同期间幂等」范式。
  5. **落地影响**：`ar_ap_items.source_type` 在 DB 层为自由 VARCHAR、**无 CHECK**（见 V108/V110 迁移注释），新增取值只需扩服务层白名单（`FinanceService.SOURCE_TYPES`）并补计费与出单路径；**本轮不改代码、不改迁移、不改 OpenAPI**。
- **`energy_fee_basis` 口径对齐（表结构不变）**：`contracts.energy_fee_basis` 的 4 值 CHECK（`none`/`by_contract`/`by_area`/`by_usage`）来自 V162，**本次不改 DB CHECK 与表结构**；但业务口径按上述调整——`none` 不结算；`by_contract` 取合同 `rent_terms.energyFeeCent`；**`by_area`（按商户面积分摊公区能耗费）与 `by_usage`（按商户用量比例分摊公区能耗费）作为「公区能耗向商户分摊」的口径已过期作废**，公区能耗不再向商户分摊、不再据此生成商户应收；商户侧能源费一律走「独立表计实际用量实际费用代收」，差额部分走电损费/水损费独立计收。存量取 `by_area`/`by_usage` 的合同按新口径执行；**DB 取值可直接收敛**——2026-08-04 第三轮用户裁决 UD-07 / DEC-R6-28 明确当前为研发环境、**无生产存量数据、测试数据可直接清理**，故 CHECK 直接收敛为 `none`/`by_contract` 两值即可，**不单独立项做历史数据迁移／回填**（收敛迁移本身仍需排期执行）。
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

## 6. 待建一览（2026-08-04 本轮登记）

> 口径已按 HTML 写实，落地未做；本轮**不改代码、迁移、OpenAPI、CSV 与三份 HTML**。

| 编号 | 待建/待对齐项 | 现状 | 需要的动作 | 依据 |
|---|---|---|---|---|
| A | **电损费 / 水损费独立计收** | 无 `source_type`、无配置项、无独立发票标识、结算只出 `energy_fee` | 补计费对象/公式/配置项（固定金额或比例，按项目可配、须配上下限）＋独立 `power_loss_fee`/`water_loss_fee` 与独立发票标识（见 §5） | ENUM L131、BSM L1259、PGM L760 |
| B | **复杂类工单「主管验收 + 提单方确认」双闸** | `acceptance_by` 三选一，表达不了与关系 | 枚举扩 `supervisor_and_reporter` 或加独立标志位 `require_reporter_confirm`，并定「复杂类」判定口径（见 §1/§3） | BSM L1235 |
| C | **商户服务工单分表独立七态**（2026-08-04 裁决改向：原「收敛到 V166」已撤销） | V022 商户服务工单表现有状态取值未与 ENUM 独立七态逐值核对 | 补齐 V022 独立七态 DB CHECK 与状态机；看板按 ENUM「工单看板分行对照」分行；台账 CSV ADM-MER-001／004 端点与状态机列按**分表**口径同步（见 §0/§3） | 第三轮用户裁决 UD-06 / DEC-R6-26；ENUM「商户服务工单状态」「工单看板分行对照」条 |
| D | **`energy_fee_basis` 收敛迁移** | DB CHECK 仍 4 值；新写入已按硬约束禁用 `by_area`/`by_usage` | **直接收敛 DB CHECK 为 `none`/`by_contract` 两值**（研发环境无存量数据，测试数据直接清理，不做历史迁移／回填立项） | 第三轮用户裁决 UD-07 / DEC-R6-28；ENUM「能源费与损耗费」条、PGM 能耗运营细则 |
| E | **工单来源补 `customer`** | `facility_work_orders.source` CHECK 现 4 值、无 `customer` | CHECK 扩 `customer`；客人端投诉直接落工单、客人为提单方、关闭须其确认（见 §1 `source` 行） | 第三轮用户裁决 UD-03 / DEC-R6-14；BSM 物业服务章·主要功能 |
| F | **工单九态分支语义落地** | `acceptance_by` 三选一，表达不了「主管验收 ＋ 提单方确认」双闸；`accepted` 现不可达 | 按 UD-05 / DEC-R6-13 三类分支实现：无争议类完成即结、投诉类提单方确认、复杂类双闸；并定「复杂类」判定口径（与本表 B 项同一改造） | 第三轮用户裁决 UD-05 / DEC-R6-13；ENUM「物业工单状态」条 |

**已裁决清单（原 B 类五项，2026-08-04 第三轮用户裁决逐项定稿）**

1. **客人投诉进入工单域的落点** —— **已裁决（UD-03 / DEC-R6-14）**：`source` **补 `customer`**，客人端投诉直接落工单，客人即提单方、关闭须其确认。CHECK 扩值列入排期（见 §1 `source` 行、本节待建一览 E）。
2. **巡检类型是否含「能耗抄表」** —— **已裁决（UD-04 / DEC-R6-12）**：**四类**（安防、消防、设施设备、环境卫生），**删「能耗抄表」**，抄表走能耗域独立流程。DB 现为 4 值，与裁决一致，无需改造（见 §0、§1 `inspection_type` 行）。
3. **工单九态语义** —— **已裁决（UD-05 / DEC-R6-13）**：**按类型分支适用**——无争议类完成即结、投诉类须提单方确认、复杂类须主管验收 ＋ 提单方确认双闸；`accepted` 在复杂类分支下可达。双闸取值与「复杂类」判定口径改造列入排期（见 §1 `status` 行、本节待建一览 B/F）。
4. **商户服务工单是否与设施工单同表** —— **已裁决（UD-06 / DEC-R6-26）**：**彻底分表、独立七态**，撤销原「统一主表」口径；商户服务工单主写 V022，看板分行对照。分表改造列入排期（见 §0、§1 表后说明、§3、本节待建一览 C）。
5. **`by_area`/`by_usage` 存量合同处置** —— **已裁决（UD-07 / DEC-R6-28）**：研发环境**无存量数据**、测试数据可直接清理，**直接收敛 DB 取值**为 `none`/`by_contract`，不为历史迁移／回填单独立项（见 §1 `energy_fee_basis` 行、§5、本节待建一览 D）。

> 落地批次：`docs/prd-audit-fix-change-log-20260804.md` §12（第三轮裁决落地 R6-B）；裁决原文见 `docs/prd-audit/business-decision-ledger.md`「第三轮裁决」节。
