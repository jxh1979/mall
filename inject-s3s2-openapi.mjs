// 一次性注入 ③ 楼宇运营 S2：楼宇维保工单 4 端点（list/create/assign/status）。镜像 s3s1 注入范式。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });

spec.paths['/api/v1/facility/work-orders'] = {
  get: {
    tags: ['facility'], operationId: 'listFacilityWorkOrders', summary: '楼宇维保工单清单',
    'x-permission': 'facility.work_order.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.work_order.viewed', 'x-audit-event-failure': 'facility.work_order.view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
  post: {
    tags: ['facility'], operationId: 'createFacilityWorkOrder', summary: '新建楼宇维保/报修工单',
    description: '③ S2：报修建单，按 priority 自动推算 sla_due_at（critical 4h/high 24h/medium 72h/low 168h）。功能标志 facility.work_order.enabled 默认关。',
    'x-permission': 'facility.work_order.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.work_order.created', 'x-audit-event-failure': 'facility.work_order.create_failed',
    'x-auth-context': 'staff-token', requestBody: { $ref: '#/components/requestBodies/FacilityWorkOrderRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/facility/work-orders/{work_order_id}/assign'] = {
  post: {
    tags: ['facility'], operationId: 'assignFacilityWorkOrder', summary: '楼宇维保工单派单',
    'x-permission': 'facility.work_order.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.work_order.assigned', 'x-audit-event-failure': 'facility.work_order.assign_failed',
    'x-auth-context': 'staff-token',
    parameters: [{ name: 'work_order_id', in: 'path', required: true, schema: { type: 'string', maxLength: 64 }, description: '工单 id。' }],
    requestBody: { $ref: '#/components/requestBodies/FacilityWorkOrderAssignRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/facility/work-orders/{work_order_id}/status'] = {
  post: {
    tags: ['facility'], operationId: 'updateFacilityWorkOrderStatus', summary: '楼宇维保工单状态流转（in_progress/resolved/closed）',
    'x-permission': 'facility.work_order.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.work_order.status_changed', 'x-audit-event-failure': 'facility.work_order.status_change_failed',
    'x-auth-context': 'staff-token',
    parameters: [{ name: 'work_order_id', in: 'path', required: true, schema: { type: 'string', maxLength: 64 }, description: '工单 id。' }],
    requestBody: { $ref: '#/components/requestBodies/FacilityWorkOrderStatusRequest' }, responses: ok,
  },
};

spec.components.requestBodies.FacilityWorkOrderRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/FacilityWorkOrderRequest' } } },
};
spec.components.requestBodies.FacilityWorkOrderAssignRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/FacilityWorkOrderAssignRequest' } } },
};
spec.components.requestBodies.FacilityWorkOrderStatusRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/FacilityWorkOrderStatusRequest' } } },
};
spec.components.schemas.FacilityWorkOrderRequest = {
  type: 'object', description: '请求载荷 FacilityWorkOrderRequest（③ S2 楼宇维保工单建单）。', additionalProperties: false,
  required: ['title', 'category'],
  properties: {
    idempotency_key: idem,
    equipment_id: str('关联设备 id（可空，非设备类报修留空）。', 64),
    space_id: str('位置空间 id。', 64),
    title: str('工单标题。'),
    description: str('问题描述。', 2000),
    category: { type: 'string', description: '工单类别。', enum: ['repair', 'maintenance', 'inspection_followup'] },
    priority: { type: 'string', description: '优先级（空=medium）。', enum: ['low', 'medium', 'high', 'critical'] },
    reported_by: str('报修人 id。', 64),
  },
};
spec.components.schemas.FacilityWorkOrderAssignRequest = {
  type: 'object', description: '请求载荷 FacilityWorkOrderAssignRequest（③ S2 派单）。', additionalProperties: false,
  required: ['assignee'],
  properties: { idempotency_key: idem, assignee: str('受理人 id。', 64) },
};
spec.components.schemas.FacilityWorkOrderStatusRequest = {
  type: 'object', description: '请求载荷 FacilityWorkOrderStatusRequest（③ S2 状态流转，目标态）。', additionalProperties: false,
  required: ['status'],
  properties: { idempotency_key: idem, status: { type: 'string', description: '目标状态。', enum: ['in_progress', 'resolved', 'closed'] } },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ③S2: 3 paths(4 ops) + 3 requestBodies + 3 schemas');
