// 一次性注入 集团治理 access 新端点 + analytics groupBy：8 new paths + 11 new ops + 1 existing op param。
// 用法：node inject-governance-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';

const path = process.argv[2] || 'docs/openapi/openapi-p0.json';
const spec = JSON.parse(readFileSync(path, 'utf8'));

spec.components ??= {};
spec.components.requestBodies ??= {};
spec.components.schemas ??= {};

const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '401': err, '403': err, '404': err, '409': err, '500': err };
const page = { '200': { $ref: '#/components/responses/Page' }, '401': err, '403': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });
const id = (d) => str(d, 64);
const int = (d, min) => ({ type: 'integer', description: d, minimum: min });
const dateTime = (d) => ({ type: 'string', format: 'date-time', description: d });
const arr = (d, item, maxItems = 50) => ({ type: 'array', description: d, items: item, maxItems });
const pathParam = (name, d) => ({ name, in: 'path', required: true, description: d, schema: { type: 'string', maxLength: 64 } });
const query = (name, d, schema) => ({ name, in: 'query', required: false, description: d, schema });

function op({ id, perm, summary, desc, idemReq = false, evOk, evFail, body, params = [], list = false }) {
  const o = {
    tags: ['access'],
    operationId: id,
    summary,
    'x-permission': perm,
    'x-data-scope': 'project',
    'x-terminal-allowed': web,
    'x-idempotency-required': idemReq,
    'x-audit-event-success': evOk,
    'x-audit-event-failure': evFail,
    'x-auth-context': 'staff-token',
  };
  if (desc) o.description = desc;
  if (params.length) o.parameters = params;
  if (body) o.requestBody = { $ref: '#/components/requestBodies/' + body };
  o.responses = list ? page : ok;
  return o;
}

const rb = (name) => ({ required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/' + name } } } });
for (const name of [
  'RoleAssignmentRequest',
  'PositionRoleTemplateRequest',
  'UserPositionAssignmentRequest',
  'AccessGrantApplyRequest',
  'AccessGrantReviewRequest',
]) {
  spec.components.requestBodies[name] = rb(name);
}

spec.paths['/api/v1/access/role-assignments'] = {
  post: op({ id: 'assignUserRole', perm: 'access.role.assign', summary: '为用户分配角色', idemReq: true,
    body: 'RoleAssignmentRequest', evOk: 'access.role.assigned', evFail: 'access.role.assign_failed' }),
};
spec.paths['/api/v1/access/role-assignments/unassign'] = {
  post: op({ id: 'unassignUserRole', perm: 'access.role.assign', summary: '取消用户角色', idemReq: true,
    body: 'RoleAssignmentRequest', evOk: 'access.role.unassigned', evFail: 'access.role.unassign_failed' }),
};
spec.paths['/api/v1/access/position-role-templates'] = {
  get: op({ id: 'listPositionRoleTemplates', perm: 'access.role.assign', summary: '岗位角色模板列表', list: true,
    evOk: 'access.position_role_template.viewed', evFail: 'access.position_role_template.view_failed' }),
  post: op({ id: 'createPositionRoleTemplate', perm: 'access.role.assign', summary: '创建岗位角色模板', idemReq: true,
    body: 'PositionRoleTemplateRequest', evOk: 'access.position_role_template.saved', evFail: 'access.position_role_template.save_failed' }),
};
spec.paths['/api/v1/access/position-role-templates/{id}'] = {
  put: op({ id: 'updatePositionRoleTemplate', perm: 'access.role.assign', summary: '更新岗位角色模板', idemReq: true,
    params: [pathParam('id', '岗位角色模板 id。')], body: 'PositionRoleTemplateRequest',
    evOk: 'access.position_role_template.saved', evFail: 'access.position_role_template.save_failed' }),
  delete: op({ id: 'deletePositionRoleTemplate', perm: 'access.role.assign', summary: '删除岗位角色模板',
    params: [pathParam('id', '岗位角色模板 id。')],
    evOk: 'access.position_role_template.deleted', evFail: 'access.position_role_template.delete_failed' }),
};
spec.paths['/api/v1/access/users/{id}/position'] = {
  put: op({ id: 'assignUserPosition', perm: 'access.role.assign', summary: '分配用户岗位并套用岗位权限', idemReq: true,
    desc: '目标 user_id 由路径参数提供；后端按岗位+职级匹配岗位角色模板并分配角色/权限模板。',
    params: [pathParam('id', '目标用户 id。')], body: 'UserPositionAssignmentRequest',
    evOk: 'access.user_position.assigned', evFail: 'access.user_position.assign_failed' }),
};
spec.paths['/api/v1/access/grant-requests'] = {
  get: op({ id: 'listAccessGrantRequests', perm: 'access.grant_request.view', summary: '授权申请列表', list: true,
    params: [query('window_days', '只看最近 N 天；空则不限窗口。', int('天数。', 1))],
    evOk: 'access.grant_request.viewed', evFail: 'access.grant_request.view_failed' }),
  post: op({ id: 'createAccessGrantRequest', perm: 'access.grant_request.apply', summary: '创建需主管复核的授权申请', idemReq: true,
    body: 'AccessGrantApplyRequest', evOk: 'access.grant_request.applied', evFail: 'access.grant_request.apply_failed' }),
};
spec.paths['/api/v1/access/grant-requests/{id}/review'] = {
  post: op({ id: 'reviewAccessGrantRequest', perm: 'access.grant_request.review', summary: '复核授权申请', idemReq: true,
    desc: '除 access.grant_request.review 外，还必须具备 governance.supervisor_review.approve。',
    params: [pathParam('id', '授权申请 id。')], body: 'AccessGrantReviewRequest',
    evOk: 'access.grant_request.reviewed', evFail: 'access.grant_request.review_failed' }),
};
spec.paths['/api/v1/access/grant-requests/summary'] = {
  get: op({ id: 'getAccessGrantSummary', perm: 'access.grant_request.view', summary: '授权申请治理汇总',
    params: [
      query('window_days', '统计最近 N 天，默认 30。', int('天数。', 1)),
      query('position_threshold', '岗位维度推荐阈值。', int('次数阈值。', 1)),
      query('user_threshold', '用户异常阈值。', int('次数阈值。', 1)),
    ],
    evOk: 'access.grant_request.summary_viewed', evFail: 'access.grant_request.summary_view_failed' }),
};

const groupPath = '/api/v1/analytics/group-overview';
const groupOp = spec.paths[groupPath]?.get;
if (!groupOp) {
  throw new Error(`${groupPath} GET is missing`);
}
groupOp.parameters = (groupOp.parameters || []).filter((p) => p.name !== 'groupBy');
groupOp.parameters.push({
  name: 'groupBy',
  in: 'query',
  required: false,
  schema: { type: 'string', enum: ['project', 'entity'] },
  description: '汇总维度：project(默认)按项目 / entity 按法律主体下钻。',
});

const S = spec.components.schemas;
S.RoleAssignmentRequest = {
  type: 'object', description: '用户角色分配/取消请求。', additionalProperties: false,
  required: ['idempotency_key', 'user_id', 'role_id'],
  properties: {
    idempotency_key: idem,
    operator_note: str('操作备注。', 1000),
    user_id: id('目标用户 id。'),
    role_id: id('角色 id。'),
  },
};
S.PositionRoleTemplateRequest = {
  type: 'object', description: '岗位角色模板请求。', additionalProperties: false,
  required: ['idempotency_key', 'position_code', 'staff_level_id', 'role_id', 'template_id'],
  properties: {
    idempotency_key: idem,
    operator_note: str('操作备注。', 1000),
    position_code: str('岗位编码。', 120),
    staff_level_id: id('职级 id。'),
    role_id: id('角色 id。'),
    template_id: id('权限模板 id。'),
    status: { type: 'string', description: '模板状态，默认 active。', enum: ['active', 'inactive'] },
  },
};
S.UserPositionAssignmentRequest = {
  type: 'object', description: '用户岗位分配请求；user_id 由路径参数提供，不在请求体中重复提交。', additionalProperties: false,
  required: ['idempotency_key', 'position_code', 'staff_level_id'],
  properties: {
    idempotency_key: idem,
    operator_note: str('操作备注。', 1000),
    position_code: str('岗位编码。', 120),
    staff_level_id: id('职级 id。'),
    base_salary_amount_cent: { type: 'integer', format: 'int64', minimum: 0, description: '具体薪资，整数分。' },
    salary_currency: { type: 'string', description: '薪资币种；P0 仅 CNY。', enum: ['CNY'] },
    salary_detail: str('薪资说明。', 1000),
  },
};
S.AccessGrantApplyRequest = {
  type: 'object', description: '需主管复核的授权申请请求。', additionalProperties: false,
  required: ['idempotency_key', 'request_type', 'target_user_id'],
  properties: {
    idempotency_key: idem,
    operator_note: str('操作备注。', 1000),
    evidence_attachment_ids: arr('证据附件 id。', id('附件 id。')),
    request_type: { type: 'string', description: '申请类型。', enum: ['user_role', 'user_position', 'temporary_permission'] },
    target_user_id: id('目标用户 id。'),
    role_id: id('角色 id；user_role 类型必填。'),
    position_code: str('岗位编码；user_position 类型必填。', 120),
    staff_level_id: id('职级 id；user_position 类型必填。'),
    base_salary_amount_cent: { type: 'integer', format: 'int64', minimum: 0, description: '具体薪资，整数分。' },
    salary_currency: { type: 'string', description: '薪资币种；P0 仅 CNY。', enum: ['CNY'] },
    salary_detail: str('薪资说明。', 1000),
    permission_codes: arr('临时权限码；temporary_permission 类型必填。', str('权限码。', 120), 200),
    valid_from: dateTime('临时授权生效时间。'),
    expires_at: dateTime('临时授权失效时间。'),
    single_use: { type: 'boolean', description: '是否单次使用。' },
    reason: str('申请原因。', 1000),
  },
};
S.AccessGrantReviewRequest = {
  type: 'object', description: '授权申请复核请求。', additionalProperties: false,
  required: ['idempotency_key', 'decision'],
  properties: {
    idempotency_key: idem,
    operator_note: str('操作备注。', 1000),
    evidence_attachment_ids: arr('证据附件 id。', id('附件 id。')),
    decision: { type: 'string', description: '复核决策。', enum: ['approve', 'reject'] },
    comment: str('复核意见。', 1000),
  },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected 集团治理: 8 paths(11 ops) + analytics groupBy + 5 requestBodies + 5 schemas');
