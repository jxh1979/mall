# 楼宇运营业务域 PRD（V1.0·据需求重建）

> 生成：2026-06-27。依据：M1 需求采集 §3-§6 + 业务 HTML §十（地产运营：设备/物业工单/能耗/巡检枚举）+ `platform-governance-map-for-operations-v0.1.html`「楼宇运营」+ `business-system-map-for-operations-v0.1.html`「物业服务/楼宇运营」+ V165-V168 迁移 + `com.complex.platform.facility` / `com.complex.platform.energy` Controller 与 Service。本文向业务 HTML §十对齐；冲突以业务 HTML 为最终解释权，本文同时逐字登记本轮真实 DB CHECK 与实际端点。
> 重建动因：据需求重建，旧骨架枚举漂移(设备3→6态/工单类型3→6/表计3→4/巡检4技术态→7业务态)已纠正。楼宇本体运营不能继续混在员工工作设备、游乐设备或旧商户服务工单里；V165-V168 已把楼宇机电设备、维保/物业工单、楼宇巡检、能耗抄表拆成独立闭环，本文补齐产品/技术基线，避免后续再用旧 V022 工单或游乐安全巡检替代楼宇域。

## 0. 定位与边界
- 楼宇运营 = 综合体建筑本身的机电设备台账、维保/物业工单、楼宇巡检、能耗抄表与能源费应收。
- 数据隔离口径 = 综合体/项目级。表上保留 `project_id`，多数写路径在 Service 层调用 `authorizationService.requirePlatformScope()`，商户侧自助提单通过 `source=merchant_self` 进入同一工单表。
- 楼宇设备不同于员工工作设备、游乐设备：本域只管电梯/暖通/配电/给排水消防等建筑机电设施。
- V166 工单统一承接设备告警、巡检异常、商户报修/保洁/投诉/其他服务请求；旧 `merchant_service_work_orders` 视为历史死表，不在 V166 DROP。
- 能耗抄表独立归 `energy` 域；业务 HTML §十曾把“能耗抄表”列入巡检类型，但 V167 明确“能耗抄表不在巡检域，归能耗域”，本 PRD 按真实 DB CHECK 登记。
- 不做：具体 IoT 协议接入、第三方远程表厂商对接、备件库存采购闭环；这些只保留字段或上下游连接点。

## 1. 业务对象与状态枚举（对齐 HTML §十，逐字落 DB CHECK）
| 对象 | 枚举/字段 | 取值（DB CHECK / 服务校验原值） | 业务说明 |
|---|---|---|---|
| 设施设备 `facility_equipment` | `equipment_type` | `elevator` / `hvac` / `power` / `water_fire` | 电梯扶梯 / 暖通空调 / 配电供电 / 给排水消防 |
| 设施设备 `facility_equipment` | `status` | `normal` / `alarm` / `fault` / `maintenance` / `disabled` / `retired` | 正常 / 告警 / 故障 / 维保中 / 停用 / 报废 |
| 设施设备 `facility_equipment` | `alarm_source` | `manual` / `iot` | 人工上报 / 远程或物联自动采集；仅告警时填写 |
| 工单策略 `facility_work_order_policy`、工单 `facility_work_orders` | `work_order_type` | `repair` / `cleaning` / `complaint` / `rectification` / `inspection` / `other` | 报修 / 保洁 / 投诉 / 整改 / 巡检 / 其他服务请求 |
| 工单策略/工单 | `priority` | `urgent` / `high` / `normal` | 用于匹配 SLA「类型 × 优先级」 |
| 工单策略 | `acceptance_by` | `handler` / `supervisor` / `reporter` | 处理人完成即结 / 主管验收 / 提单方确认 |
| 工单 `facility_work_orders` | `source` | `merchant_self` / `platform` / `equipment_alarm` / `inspection` | 商户自助 / 平台发起 / 设备告警 / 巡检异常 |
| 工单 `facility_work_orders` | `status` | `pending_assign` / `assigned` / `processing` / `pending_review` / `pending_acceptance` / `accepted` / `closed` / `suspended` / `overtime` | 待派单 / 已派单 / 处理中 / 待回访 / 待验收 / 已验收 / 已关闭 / 已挂起 / 已超时；`accepted` 为 DB 保留值，当前服务验收后直接写 `closed` |
| 巡检模板/任务 | `inspection_type` | `security` / `fire` / `facility` / `environment` | 安防 / 消防 / 设施设备 / 环境卫生；能耗抄表不落巡检 CHECK |
| 巡检模板 | `abnormal_mode` | `light` / `to_work_order` | 轻记录闭环 / 转整改工单闭环 |
| 巡检任务 `facility_inspection_tasks` | `status` | `pending` / `in_progress` / `normal` / `abnormal` / `rectifying` / `rechecked` / `closed` | 待执行 / 执行中 / 正常 / 发现异常 / 整改中 / 已复检 / 已关闭 |
| 巡检任务 | `schedule_mode` | `template` / `manual` / `route` | 模板自动排 / 主管手动排 / 点位路线排 |
| 巡检记录 `facility_inspection_records` | `result` | `normal` / `abnormal` | 正常 / 异常 |
| 能耗表计 `energy_meters` | `meter_type` | `electricity` / `water` / `gas` / `heat_cold` | 电表 / 水表 / 燃气表 / 冷热量表 |
| 能耗表计 `energy_meters` | `gather_object` | `public_area` / `merchant` / `equipment_circuit` | 公共区域 / 商户 / 设备或回路 |
| 能耗表计/读数 | `read_mode` | `remote` / `manual` | 远程自动读 / 人工抄表 |
| 抄表读数 `energy_meter_readings` | `status` | `pending` / `recorded` / `reviewed` / `abnormal` | 待抄 / 已抄录 / 已审核 / 异常 |
| 抄表读数 `energy_meter_readings` | `abnormal_reason` | `surge` / `drop` / `zero` / `reverse` | 突增 / 突降 / 零读数 / 读数倒走 |
| 合同 `contracts` | `energy_fee_basis` | `none` / `by_contract` / `by_area` / `by_usage` | 公区能源费不分摊 / 按合同约定 / 按面积分摊 / 按用量比例；CHECK 来自 V162，结算由 V168 能耗域读取 |

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
- 默认策略：报修/整改按主管验收，保洁/巡检/其他按处理人完成即结，投诉按提单方确认；后台可通过 `POST /policies` 调整。
- 创建工单时根据策略写 `response_due_at`、`handle_due_at`；`mark-overdue` 扫描超期未关闭工单并置 `overtime=true`。
- 商户自助提单只允许 `repair`、`cleaning`、`complaint`、`other`；`rectification` 与 `inspection` 由平台、设备告警或巡检异常发起。
- 主流程：`pending_assign` → `assigned` → `processing`。完成时按 `acceptance_by` 分流：`handler` 直接 `closed`，`supervisor` 进 `pending_acceptance`，`reporter` 进 `pending_review`。
- `accept` / `review` 可记录 `satisfaction` 1-5，关闭时写 `verified_at`、`verifier_id`，并对关联 `equipment_id` 的设备回写 `normal`。
- 支持 `suspend` / `resume`；只有活动态可挂起，挂起后恢复为 `processing`。
- 事件解耦：设备 `alarm/fault` 自动转 `repair/high`；巡检异常 `to_work_order` 自动转 `rectification/normal` 并把 `work_order_id` 回写巡检任务。

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
- 历史表：`merchant_service_work_orders` / `merchant_service_work_order_actions` 保留但停止作为楼宇工单主写表
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
- `zero`、`reverse`、`surge`、`drop` 均进入 `status=abnormal`，通过 `review` 复核为 `reviewed`；异常不是拒收。
- 异常阈值表 `energy_anomaly_thresholds` 按 `meter_type` 配置，V168 默认四类各 50%；当前未暴露独立阈值 REST 端点。
- 能源费结算读取有效合同 `contracts.energy_fee_basis`：`none` 不结算；`by_contract` 取合同 `rent_terms.energyFeeCent`；`by_area` 按商户面积分摊公共区域能耗费；`by_usage` 按商户用量比例分摊。
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
