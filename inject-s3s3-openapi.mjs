// 一次性注入 ③ 楼宇运营 S3：楼宇巡检 5 端点（模板 list/create + 任务 list/generate/complete）。镜像 s3s1/s3s2 注入范式。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });
const arr = (d) => ({ type: 'array', description: d, items: { type: 'string', maxLength: 255 } });

spec.paths['/api/v1/facility/inspection-templates'] = {
  get: {
    tags: ['facility'], operationId: 'listFacilityInspectionTemplates', summary: '楼宇巡检模板清单',
    'x-permission': 'facility.inspection.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.inspection.template_viewed', 'x-audit-event-failure': 'facility.inspection.template_view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
  post: {
    tags: ['facility'], operationId: 'createFacilityInspectionTemplate', summary: '新建楼宇巡检模板',
    description: '③ S3：定义巡检项清单（JSONB）。功能标志 facility.inspection.enabled 默认关。',
    'x-permission': 'facility.inspection.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.inspection.template_created', 'x-audit-event-failure': 'facility.inspection.template_create_failed',
    'x-auth-context': 'staff-token', requestBody: { $ref: '#/components/requestBodies/FacilityInspectionTemplateRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/facility/inspection-tasks'] = {
  get: {
    tags: ['facility'], operationId: 'listFacilityInspectionTasks', summary: '楼宇巡检任务清单（含超期）',
    'x-permission': 'facility.inspection.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.inspection.task_viewed', 'x-audit-event-failure': 'facility.inspection.task_view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
};
spec.paths['/api/v1/facility/inspection-tasks/generate'] = {
  post: {
    tags: ['facility'], operationId: 'generateFacilityInspectionTasks', summary: '按设备维保周期生成到期巡检任务',
    description: '③ S3：对每台 active 且设维保周期的设备按 last_maintenance_at+周期 推 due_at 生成待办（自然键防重），并翻超期。亦由 @Scheduled 作业触发。',
    'x-permission': 'facility.inspection.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.inspection.tasks_generated', 'x-audit-event-failure': 'facility.inspection.tasks_generate_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Success' }, '403': err, '409': err, '500': err },
  },
};
spec.paths['/api/v1/facility/inspection-tasks/{task_id}/complete'] = {
  post: {
    tags: ['facility'], operationId: 'completeFacilityInspectionTask', summary: '完成巡检任务并写巡检记录',
    'x-permission': 'facility.inspection.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.inspection.task_completed', 'x-audit-event-failure': 'facility.inspection.task_complete_failed',
    'x-auth-context': 'staff-token',
    parameters: [{ name: 'task_id', in: 'path', required: true, schema: { type: 'string', maxLength: 64 }, description: '巡检任务 id。' }],
    requestBody: { $ref: '#/components/requestBodies/FacilityInspectionCompleteRequest' }, responses: ok,
  },
};

spec.components.requestBodies.FacilityInspectionTemplateRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/FacilityInspectionTemplateRequest' } } },
};
spec.components.requestBodies.FacilityInspectionCompleteRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/FacilityInspectionCompleteRequest' } } },
};
spec.components.schemas.FacilityInspectionTemplateRequest = {
  type: 'object', description: '请求载荷 FacilityInspectionTemplateRequest（③ S3 巡检模板）。', additionalProperties: false,
  required: ['name'],
  properties: {
    idempotency_key: idem, name: str('模板名称。'),
    equipment_type: { type: 'string', description: '适用设备类型（空=通用）。', enum: ['hvac', 'elevator', 'fire', 'power', 'water', 'weak_current', 'other'] },
    items: arr('巡检项清单。'),
  },
};
spec.components.schemas.FacilityInspectionCompleteRequest = {
  type: 'object', description: '请求载荷 FacilityInspectionCompleteRequest（③ S3 完成巡检）。', additionalProperties: false,
  properties: {
    idempotency_key: idem, inspector: str('巡检人 id。', 64),
    result: { type: 'string', description: '巡检结论（空=pass）。', enum: ['pass', 'fail'] },
    findings: str('问题描述。', 2000), checked_items: arr('已检项。'),
  },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ③S3: 4 paths(5 ops) + 2 requestBodies + 2 schemas');
