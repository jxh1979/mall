// 条目3b 注入：空间类型字典 列表/录入 2 端点 + SpaceTypeRequest。用法：node inject-spacetype-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2] || 'docs/openapi/openapi-p0.json';
const spec = JSON.parse(readFileSync(path, 'utf8'));
spec.components ??= {}; spec.components.schemas ??= {}; spec.components.requestBodies ??= {};
const err = { $ref: '#/components/responses/Error' };

spec.components.schemas.SpaceTypeRequest = {
  type: 'object',
  description: '空间类型字典录入/修改请求；type_code 为唯一键，存在则更新名称/排序/状态。',
  additionalProperties: false,
  required: ['idempotency_key', 'type_code', 'type_name'],
  properties: {
    idempotency_key: { type: 'string', description: '操作幂等键。', maxLength: 128 },
    operator_note: { type: 'string', description: '操作备注。', maxLength: 255 },
    evidence_attachment_ids: { type: 'array', description: '证据附件 ID 列表。', items: { type: 'string', maxLength: 64 }, maxItems: 50 },
    type_code: { type: 'string', description: '类型编码（唯一键，如 building/shop/kiosk）。', maxLength: 64 },
    type_name: { type: 'string', description: '类型显示名（如 楼栋/商铺）。', maxLength: 120 },
    sort_order: { type: 'integer', description: '排序值，越小越靠前。', minimum: 0 },
    status: { type: 'string', description: '状态：active 启用 / disabled 停用。', maxLength: 32 },
  },
};
spec.components.requestBodies.SpaceTypeRequest = {
  required: true,
  content: { 'application/json': { schema: { $ref: '#/components/schemas/SpaceTypeRequest' } } },
};

spec.paths['/api/v1/platform/space-types'] = {
  get: {
    tags: ['platform'], operationId: 'listSpaceTypes', summary: '空间类型字典列表',
    description: '列出空间类型字典（含停用），供前端空间类型下拉与后台字典管理。',
    'x-permission': 'governance.space.view', 'x-data-scope': 'platform', 'x-terminal-allowed': ['web'],
    'x-idempotency-required': false, 'x-audit-event-success': 'space_type.viewed',
    'x-audit-event-failure': 'space_type.view_failed', 'x-auth-context': 'staff-token',
    responses: { '200': { $ref: '#/components/responses/Page' }, '401': err, '403': err, '500': err },
  },
  post: {
    tags: ['platform'], operationId: 'createSpaceType', summary: '录入/修改空间类型字典',
    description: '后台管理员录入或修改空间类型字典项（按 type_code upsert）；新增/启用后即可用于空间 space_type 硬校验。',
    'x-permission': 'governance.space.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': ['web'],
    'x-idempotency-required': true, 'x-audit-event-success': 'space_type.updated',
    'x-audit-event-failure': 'space_type.update_failed', 'x-auth-context': 'staff-token',
    requestBody: { $ref: '#/components/requestBodies/SpaceTypeRequest' },
    responses: { '200': { $ref: '#/components/responses/Success' }, '400': err, '409': err, '500': err },
  },
};
writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected space-types list/create ops + SpaceTypeRequest');
