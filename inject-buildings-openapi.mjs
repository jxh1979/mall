// 一次性注入 楼宇运营（facility + energy）OpenAPI：31 paths + 38 ops。
// 用法：node inject-buildings-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';

const path = process.argv[2] || 'docs/openapi/openapi-p0.json';
const spec = JSON.parse(readFileSync(path, 'utf8'));

spec.components ??= {};
spec.components.requestBodies ??= {};
spec.components.schemas ??= {};

const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '401': err, '403': err, '404': err, '409': err, '500': err };
const page = { '200': { $ref: '#/components/responses/Page' }, '401': err, '403': err, '500': err };
const single = { '200': { $ref: '#/components/responses/Success' }, '401': err, '403': err, '404': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });
const id = (d) => str(d, 64);
const date = (d) => ({ type: 'string', format: 'date', description: d });
const dateTime = (d) => ({ type: 'string', format: 'date-time', description: d });
const int = (d, min, max) => {
  const s = { type: 'integer', description: d };
  if (min !== undefined) s.minimum = min;
  if (max !== undefined) s.maximum = max;
  return s;
};
const num = (d, min, max) => {
  const s = { type: 'number', description: d };
  if (min !== undefined) s.minimum = min;
  if (max !== undefined) s.maximum = max;
  return s;
};
const arr = (d, item, maxItems) => {
  const s = { type: 'array', description: d, items: item };
  if (maxItems) s.maxItems = maxItems;
  return s;
};
const objectItem = { type: 'object', additionalProperties: true };
const pathParam = (name, d) => ({ name, in: 'path', required: true, description: d, schema: { type: 'string', maxLength: 64 } });
const query = (name, d, schema, required = false) => ({ name, in: 'query', required, description: d, schema });
const idempotencyQuery = (required) => query('idempotency_key', '操作幂等键。', { type: 'string', maxLength: 128 }, required);

function op({ tag, id, perm, summary, desc, idemReq = false, evOk, evFail, body, params = [], list = false, one = false }) {
  const o = {
    tags: [tag],
    operationId: id,
    summary,
    'x-permission': perm,
    'x-data-scope': 'platform',
    'x-terminal-allowed': web,
    'x-idempotency-required': idemReq,
    'x-audit-event-success': evOk,
    'x-audit-event-failure': evFail,
    'x-auth-context': 'staff-token',
  };
  if (desc) o.description = desc;
  if (params.length) o.parameters = params;
  if (body) o.requestBody = { $ref: '#/components/requestBodies/' + body };
  o.responses = list ? page : one ? single : ok;
  return o;
}

const rb = (name) => ({ required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/' + name } } } });
for (const name of [
  'FacilityEquipmentRequest',
  'FacilityEquipmentStatusRequest',
  'FacilityWorkOrderRequest',
  'FacilityWorkOrderAssignRequest',
  'FacilityWorkOrderActionRequest',
  'FacilityWorkOrderPolicyRequest',
  'FacilityInspectionTemplateRequest',
  'FacilityInspectionTaskRequest',
  'FacilityInspectionActionRequest',
  'FacilityInspectionRecordSubmitRequest',
  'EnergyMeterRequest',
  'EnergyReadingRecordRequest',
  'EnergyReadingReviewRequest',
  'EnergySettleRequest',
]) {
  spec.components.requestBodies[name] = rb(name);
}

// ---- facility equipment ----
spec.paths['/api/v1/facility/equipment'] = {
  get: op({ tag: 'facility', id: 'listFacilityEquipment', perm: 'facility.equipment.manage', summary: '设备台账列表', list: true,
    evOk: 'facility.equipment.viewed', evFail: 'facility.equipment.view_failed' }),
  post: op({ tag: 'facility', id: 'createFacilityEquipment', perm: 'facility.equipment.manage', summary: '创建设备台账', idemReq: true,
    body: 'FacilityEquipmentRequest', evOk: 'facility.equipment_created', evFail: 'facility.equipment_create_failed' }),
};
spec.paths['/api/v1/facility/equipment/maintenance-due'] = {
  get: op({ tag: 'facility', id: 'listFacilityMaintenanceDueEquipment', perm: 'facility.equipment.manage', summary: '查询维保到期设备', list: true,
    evOk: 'facility.equipment.maintenance_due_viewed', evFail: 'facility.equipment.maintenance_due_view_failed' }),
};
spec.paths['/api/v1/facility/equipment/{equipment_id}'] = {
  post: op({ tag: 'facility', id: 'updateFacilityEquipment', perm: 'facility.equipment.manage', summary: '更新设备台账',
    params: [pathParam('equipment_id', '设备 id。')], body: 'FacilityEquipmentRequest',
    evOk: 'facility.equipment_updated', evFail: 'facility.equipment_update_failed' }),
};
spec.paths['/api/v1/facility/equipment/{equipment_id}/status'] = {
  post: op({ tag: 'facility', id: 'changeFacilityEquipmentStatus', perm: 'facility.equipment.manage', summary: '变更设备状态/上报告警',
    params: [pathParam('equipment_id', '设备 id。')], body: 'FacilityEquipmentStatusRequest',
    evOk: 'facility.equipment_status_changed', evFail: 'facility.equipment_status_change_failed' }),
};
spec.paths['/api/v1/facility/equipment/{equipment_id}/maintenance-done'] = {
  post: op({ tag: 'facility', id: 'recordFacilityEquipmentMaintenanceDone', perm: 'facility.equipment.manage', summary: '记录设备维保完成',
    params: [pathParam('equipment_id', '设备 id。'), idempotencyQuery(false)],
    evOk: 'facility.equipment_maintained', evFail: 'facility.equipment_maintain_failed' }),
};

// ---- facility work orders ----
spec.paths['/api/v1/facility/work-orders'] = {
  get: op({ tag: 'facility', id: 'listFacilityWorkOrders', perm: 'facility.work_order.manage', summary: '工单列表', list: true,
    evOk: 'facility.work_order.viewed', evFail: 'facility.work_order.view_failed' }),
  post: op({ tag: 'facility', id: 'createFacilityWorkOrder', perm: 'facility.work_order.manage', summary: '创建维保/物业工单', idemReq: true,
    body: 'FacilityWorkOrderRequest', evOk: 'facility.work_order_created', evFail: 'facility.work_order_create_failed' }),
};
spec.paths['/api/v1/facility/work-orders/overdue'] = {
  get: op({ tag: 'facility', id: 'listFacilityOverdueWorkOrders', perm: 'facility.work_order.manage', summary: '逾期工单列表', list: true,
    evOk: 'facility.work_order.overdue_viewed', evFail: 'facility.work_order.overdue_view_failed' }),
};
spec.paths['/api/v1/facility/work-orders/{work_order_id}'] = {
  get: op({ tag: 'facility', id: 'getFacilityWorkOrder', perm: 'facility.work_order.manage', summary: '工单详情', one: true,
    params: [pathParam('work_order_id', '工单 id。')], evOk: 'facility.work_order.viewed', evFail: 'facility.work_order.view_failed' }),
};
for (const [action, id, summary, evOk, evFail, body] of [
  ['assign', 'assignFacilityWorkOrder', '工单派单', 'facility.work_order_assigned', 'facility.work_order_assign_failed', 'FacilityWorkOrderAssignRequest'],
  ['start', 'startFacilityWorkOrder', '开始处理工单', 'facility.work_order_started', 'facility.work_order_start_failed', 'FacilityWorkOrderActionRequest'],
  ['complete', 'completeFacilityWorkOrder', '完成工单处理', 'facility.work_order_completed', 'facility.work_order_complete_failed', 'FacilityWorkOrderActionRequest'],
  ['accept', 'acceptFacilityWorkOrder', '主管验收工单', 'facility.work_order_accepted', 'facility.work_order_accept_failed', 'FacilityWorkOrderActionRequest'],
  ['review', 'reviewFacilityWorkOrder', '报修方回访工单', 'facility.work_order_reviewed', 'facility.work_order_review_failed', 'FacilityWorkOrderActionRequest'],
  ['suspend', 'suspendFacilityWorkOrder', '挂起工单', 'facility.work_order_suspended', 'facility.work_order_suspend_failed', 'FacilityWorkOrderActionRequest'],
  ['resume', 'resumeFacilityWorkOrder', '恢复工单', 'facility.work_order_resumed', 'facility.work_order_resume_failed', 'FacilityWorkOrderActionRequest'],
]) {
  spec.paths[`/api/v1/facility/work-orders/{work_order_id}/${action}`] = {
    post: op({ tag: 'facility', id, perm: 'facility.work_order.manage', summary, idemReq: true,
      params: [pathParam('work_order_id', '工单 id。')], body, evOk, evFail }),
  };
}
spec.paths['/api/v1/facility/work-orders/mark-overdue'] = {
  post: op({ tag: 'facility', id: 'markFacilityWorkOrdersOverdue', perm: 'facility.work_order.manage', summary: '批量标记逾期工单', idemReq: true,
    params: [idempotencyQuery(true)], evOk: 'facility.work_order_overdue_marked', evFail: 'facility.work_order_overdue_mark_failed' }),
};
spec.paths['/api/v1/facility/work-orders/policies'] = {
  post: op({ tag: 'facility', id: 'saveFacilityWorkOrderPolicy', perm: 'facility.work_order.manage', summary: '保存工单 SLA/验收策略', idemReq: true,
    body: 'FacilityWorkOrderPolicyRequest', evOk: 'facility.work_order_policy_saved', evFail: 'facility.work_order_policy_save_failed' }),
};

// ---- facility inspections ----
spec.paths['/api/v1/facility/inspections/templates'] = {
  get: op({ tag: 'facility', id: 'listFacilityInspectionTemplates', perm: 'facility.inspection.manage', summary: '巡检模板列表', list: true,
    evOk: 'facility.inspection_template.viewed', evFail: 'facility.inspection_template.view_failed' }),
  post: op({ tag: 'facility', id: 'createFacilityInspectionTemplate', perm: 'facility.inspection.manage', summary: '创建巡检模板', idemReq: true,
    body: 'FacilityInspectionTemplateRequest', evOk: 'facility.inspection_template_created', evFail: 'facility.inspection_template_create_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks'] = {
  get: op({ tag: 'facility', id: 'listFacilityInspectionTasks', perm: 'facility.inspection.manage', summary: '巡检任务列表', list: true,
    evOk: 'facility.inspection_task.viewed', evFail: 'facility.inspection_task.view_failed' }),
  post: op({ tag: 'facility', id: 'createFacilityInspectionTask', perm: 'facility.inspection.manage', summary: '创建巡检任务', idemReq: true,
    body: 'FacilityInspectionTaskRequest', evOk: 'facility.inspection_task_created', evFail: 'facility.inspection_task_create_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks/{task_id}'] = {
  get: op({ tag: 'facility', id: 'getFacilityInspectionTask', perm: 'facility.inspection.manage', summary: '巡检任务详情', one: true,
    params: [pathParam('task_id', '巡检任务 id。')], evOk: 'facility.inspection_task.viewed', evFail: 'facility.inspection_task.view_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks/{task_id}/start'] = {
  post: op({ tag: 'facility', id: 'startFacilityInspectionTask', perm: 'facility.inspection.manage', summary: '开始巡检任务', idemReq: true,
    params: [pathParam('task_id', '巡检任务 id。')], body: 'FacilityInspectionActionRequest',
    evOk: 'facility.inspection_task_started', evFail: 'facility.inspection_task_start_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks/{task_id}/records'] = {
  get: op({ tag: 'facility', id: 'listFacilityInspectionRecords', perm: 'facility.inspection.manage', summary: '巡检记录列表', list: true,
    params: [pathParam('task_id', '巡检任务 id。')], evOk: 'facility.inspection_record.viewed', evFail: 'facility.inspection_record.view_failed' }),
  post: op({ tag: 'facility', id: 'submitFacilityInspectionRecord', perm: 'facility.inspection.manage', summary: '提交巡检记录', idemReq: true,
    params: [pathParam('task_id', '巡检任务 id。')], body: 'FacilityInspectionRecordSubmitRequest',
    evOk: 'facility.inspection_record_submitted', evFail: 'facility.inspection_record_submit_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks/{task_id}/recheck'] = {
  post: op({ tag: 'facility', id: 'recheckFacilityInspectionTask', perm: 'facility.inspection.manage', summary: '复查整改后的巡检任务', idemReq: true,
    params: [pathParam('task_id', '巡检任务 id。')], body: 'FacilityInspectionActionRequest',
    evOk: 'facility.inspection_task_rechecked', evFail: 'facility.inspection_task_recheck_failed' }),
};
spec.paths['/api/v1/facility/inspections/tasks/{task_id}/supervisor-confirm'] = {
  post: op({ tag: 'facility', id: 'confirmFacilityInspectionSupervisor', perm: 'facility.inspection.manage', summary: '主管确认巡检任务关闭', idemReq: true,
    params: [pathParam('task_id', '巡检任务 id。')], body: 'FacilityInspectionActionRequest',
    evOk: 'facility.inspection_task_closed', evFail: 'facility.inspection_task_close_failed' }),
};

// ---- energy ----
spec.paths['/api/v1/energy/meters'] = {
  get: op({ tag: 'energy', id: 'listEnergyMeters', perm: 'energy.meter.manage', summary: '表计列表', list: true,
    evOk: 'energy.meter.viewed', evFail: 'energy.meter.view_failed' }),
  post: op({ tag: 'energy', id: 'createEnergyMeter', perm: 'energy.meter.manage', summary: '创建能源表计', idemReq: true,
    body: 'EnergyMeterRequest', evOk: 'energy.meter_saved', evFail: 'energy.meter_save_failed' }),
};
spec.paths['/api/v1/energy/meters/{meter_id}'] = {
  post: op({ tag: 'energy', id: 'updateEnergyMeter', perm: 'energy.meter.manage', summary: '更新能源表计',
    params: [pathParam('meter_id', '表计 id。')], body: 'EnergyMeterRequest',
    evOk: 'energy.meter_saved', evFail: 'energy.meter_save_failed' }),
};
spec.paths['/api/v1/energy/readings'] = {
  get: op({ tag: 'energy', id: 'listEnergyReadings', perm: 'energy.reading.manage', summary: '抄表读数列表', list: true,
    params: [query('period', '账期 YYYY-MM；空则查询全部。', { type: 'string', pattern: '^\\d{4}-\\d{2}$' })],
    evOk: 'energy.reading.viewed', evFail: 'energy.reading.view_failed' }),
  post: op({ tag: 'energy', id: 'recordEnergyReading', perm: 'energy.reading.manage', summary: '录入抄表读数', idemReq: true,
    body: 'EnergyReadingRecordRequest', evOk: 'energy.reading_recorded', evFail: 'energy.reading_record_failed' }),
};
spec.paths['/api/v1/energy/readings/{reading_id}/review'] = {
  post: op({ tag: 'energy', id: 'reviewEnergyReading', perm: 'energy.reading.manage', summary: '复核异常抄表读数', idemReq: true,
    params: [pathParam('reading_id', '抄表读数 id。')], body: 'EnergyReadingReviewRequest',
    evOk: 'energy.reading_reviewed', evFail: 'energy.reading_review_failed' }),
};
spec.paths['/api/v1/energy/meters/{meter_id}/difference'] = {
  get: op({ tag: 'energy', id: 'getEnergyMeterDifference', perm: 'energy.reading.manage', summary: '查询总分表差异', one: true,
    params: [pathParam('meter_id', '总表 id。'), query('period', '账期 YYYY-MM。', { type: 'string', pattern: '^\\d{4}-\\d{2}$' }, true)],
    evOk: 'energy.meter_difference.viewed', evFail: 'energy.meter_difference.view_failed' }),
};
spec.paths['/api/v1/energy/settlements/energy-fee'] = {
  post: op({ tag: 'energy', id: 'settleEnergyFee', perm: 'energy.reading.manage', summary: '按账期结算能源费', idemReq: true,
    body: 'EnergySettleRequest', evOk: 'energy.fee_settled', evFail: 'energy.fee_settle_failed' }),
};
spec.paths['/api/v1/energy/settle-energy-fee'] = {
  post: op({ tag: 'energy', id: 'settleEnergyFeeShortcut', perm: 'energy.reading.manage', summary: '按账期结算能源费(兼容路径)', idemReq: true,
    body: 'EnergySettleRequest', evOk: 'energy.fee_settled', evFail: 'energy.fee_settle_failed' }),
};

// ---- schemas ----
const S = spec.components.schemas;
S.FacilityEquipmentRequest = {
  type: 'object', description: '设备台账新建/更新请求。创建时 name 必填；更新可沿用原名称。', additionalProperties: false,
  required: ['equipment_type'],
  properties: {
    id: id('外部指定设备 id。'), idempotency_key: idem, name: str('设备名称。', 120),
    equipment_type: { type: 'string', description: '设备类型。', enum: ['elevator', 'hvac', 'power', 'water_fire'] },
    space_id: id('空间 id。'), brand: str('品牌。', 120), model: str('型号。', 120), vendor: str('供应商。', 160),
    responsible_user_id: id('责任人 user_id。'), maintenance_cycle_days: int('维保周期天数。', 1),
    install_date: date('安装日期。'), warranty_until: date('质保到期日期。'), project_id: id('综合体 id。'), merchant_id: id('商户 id。'),
  },
};
S.FacilityEquipmentStatusRequest = {
  type: 'object', description: '设备状态变更/告警上报请求。', additionalProperties: false, required: ['status'],
  properties: {
    idempotency_key: idem,
    status: { type: 'string', description: '目标状态。', enum: ['normal', 'alarm', 'fault', 'maintenance', 'disabled', 'retired'] },
    alarm_source: { type: 'string', description: '告警来源；status=alarm 时必填。', enum: ['manual', 'iot'] },
    alarm_note: str('告警说明。', 1000),
  },
};
S.FacilityWorkOrderRequest = {
  type: 'object', description: '维保/物业工单创建请求。', additionalProperties: false, required: ['idempotency_key', 'work_order_type', 'title'],
  properties: {
    idempotency_key: idem,
    work_order_type: { type: 'string', description: '工单类型。', enum: ['repair', 'cleaning', 'complaint', 'rectification', 'inspection', 'other'] },
    priority: { type: 'string', description: '优先级，默认 normal。', enum: ['urgent', 'high', 'normal'] },
    title: str('工单标题。', 160), description: str('工单描述。', 2000),
    source: { type: 'string', description: '工单来源，默认 platform。', enum: ['merchant_self', 'platform', 'equipment_alarm', 'inspection'] },
    space_id: id('空间 id。'), equipment_id: id('设备 id。'), reporter_id: id('报修人/上报人 id。'),
    reporter_type: { type: 'string', description: '上报人类型。', enum: ['staff', 'merchant', 'system'] },
    project_id: id('综合体 id。'), merchant_id: id('商户 id。'),
  },
};
S.FacilityWorkOrderAssignRequest = {
  type: 'object', description: '工单派单请求。', additionalProperties: false, required: ['idempotency_key', 'assignee_id'],
  properties: { idempotency_key: idem, assignee_id: id('处理人 user_id。') },
};
S.FacilityWorkOrderActionRequest = {
  type: 'object', description: '工单开始/完成/验收/回访/挂起/恢复动作请求。完成动作必须填写 resolution；验收/回访 satisfaction 为 1-5。', additionalProperties: false,
  required: ['idempotency_key'],
  properties: { idempotency_key: idem, resolution: str('处理结果。', 2000), satisfaction: int('满意度 1-5。', 1, 5), note: str('动作备注。', 1000) },
};
S.FacilityWorkOrderPolicyRequest = {
  type: 'object', description: '工单 SLA/验收策略请求。', additionalProperties: false,
  required: ['idempotency_key', 'work_order_type', 'priority', 'response_minutes', 'handle_minutes', 'acceptance_by'],
  properties: {
    idempotency_key: idem,
    work_order_type: { type: 'string', description: '工单类型。', enum: ['repair', 'cleaning', 'complaint', 'rectification', 'inspection', 'other'] },
    priority: { type: 'string', description: '优先级。', enum: ['urgent', 'high', 'normal'] },
    response_minutes: int('响应时限分钟。', 1), handle_minutes: int('处理时限分钟。', 1),
    requires_acceptance: { type: 'boolean', description: '是否需要验收。' },
    acceptance_by: { type: 'string', description: '验收方。', enum: ['handler', 'supervisor', 'reporter'] },
    active: { type: 'boolean', description: '是否启用。' },
  },
};
S.FacilityInspectionTemplateRequest = {
  type: 'object', description: '巡检模板请求。', additionalProperties: false, required: ['idempotency_key', 'name', 'inspection_type'],
  properties: {
    idempotency_key: idem, name: str('模板名称。', 120),
    inspection_type: { type: 'string', description: '巡检类型。', enum: ['security', 'fire', 'facility', 'environment'] },
    cycle_days: int('周期天数。', 1),
    abnormal_mode: { type: 'string', description: '异常处理方式，默认 light。', enum: ['light', 'to_work_order'] },
    checklist: arr('检查项配置。', objectItem, 200), scope_note: str('巡检范围说明。', 1000),
    active: { type: 'boolean', description: '是否启用。' }, project_id: id('综合体 id。'),
  },
};
S.FacilityInspectionTaskRequest = {
  type: 'object', description: '巡检任务请求。template 模式需 template_id；route 模式需 route_points。', additionalProperties: false,
  required: ['idempotency_key'],
  properties: {
    idempotency_key: idem, template_id: id('巡检模板 id。'),
    inspection_type: { type: 'string', description: '巡检类型；无模板时必填。', enum: ['security', 'fire', 'facility', 'environment'] },
    title: str('任务标题；无模板时必填。', 160),
    schedule_mode: { type: 'string', description: '排程方式，默认 manual。', enum: ['template', 'manual', 'route'] },
    route_points: arr('路线点位。', objectItem, 200), assignee_id: id('执行人 user_id。'),
    scheduled_date: date('计划日期。'), due_date: date('到期日期。'), project_id: id('综合体 id。'), space_id: id('空间 id。'),
  },
};
S.FacilityInspectionActionRequest = {
  type: 'object', description: '巡检任务开始/复查/主管确认请求。', additionalProperties: false,
  required: ['idempotency_key'], properties: { idempotency_key: idem, note: str('动作备注。', 1000) },
};
S.FacilityInspectionRecordSubmitRequest = {
  type: 'object', description: '巡检记录提交请求。abnormal 结果必须填写 abnormal_note。', additionalProperties: false,
  required: ['idempotency_key', 'result', 'inspector_id'],
  properties: {
    idempotency_key: idem,
    result: { type: 'string', description: '巡检结果。', enum: ['normal', 'abnormal'] },
    photos: arr('照片附件 id 或 URL。', str('照片附件 id 或 URL。', 2048), 50),
    qr_code: str('点位二维码。', 255), gps_lat: num('纬度。', -90, 90), gps_lng: num('经度。', -180, 180),
    checklist_result: arr('检查项结果。', objectItem, 200), note: str('记录备注。', 1000),
    abnormal_note: str('异常说明。', 1000), inspector_id: id('巡检人 user_id。'),
  },
};
S.EnergyMeterRequest = {
  type: 'object', description: '能源表计新建/更新请求。', additionalProperties: false, required: ['name', 'meter_type', 'gather_object'],
  properties: {
    id: id('外部指定表计 id。'), idempotency_key: idem, name: str('表计名称。', 120),
    meter_type: { type: 'string', description: '表计类型。', enum: ['electricity', 'water', 'gas', 'heat_cold'] },
    parent_meter_id: id('父级总表 id。'),
    gather_object: { type: 'string', description: '采集对象。', enum: ['public_area', 'merchant', 'equipment_circuit'] },
    space_id: id('空间 id。'), merchant_id: id('商户 id。'), project_id: id('综合体 id。'),
    read_mode: { type: 'string', description: '抄表方式，默认 remote。', enum: ['remote', 'manual'] },
    active: { type: 'boolean', description: '是否启用。' },
  },
};
S.EnergyReadingRecordRequest = {
  type: 'object', description: '抄表读数录入请求。', additionalProperties: false,
  required: ['idempotency_key', 'meter_id', 'period', 'reading_value'],
  properties: {
    idempotency_key: idem, meter_id: id('表计 id。'), period: { type: 'string', description: '账期 YYYY-MM。', pattern: '^\\d{4}-\\d{2}$' },
    reading_value: num('本期读数。'), prev_reading_value: num('上期读数；后端按历史读数重算，可为空。'),
    read_mode: { type: 'string', description: '抄表方式。', enum: ['remote', 'manual'] },
    reader_id: id('抄表人 user_id。'), read_at: dateTime('抄表时间。'),
  },
};
S.EnergyReadingReviewRequest = {
  type: 'object', description: '异常抄表复核请求。', additionalProperties: false, required: ['idempotency_key', 'reviewed_by'],
  properties: { idempotency_key: idem, reviewed_by: id('复核人 user_id。') },
};
S.EnergySettleRequest = {
  type: 'object', description: '能源费结算请求。', additionalProperties: false, required: ['idempotency_key', 'period'],
  properties: { idempotency_key: idem, period: { type: 'string', description: '账期 YYYY-MM。', pattern: '^\\d{4}-\\d{2}$' } },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected 楼宇运营: 31 paths + 38 ops + 14 requestBodies + 14 schemas');
