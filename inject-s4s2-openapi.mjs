// 一次性注入 ④ 集团治理 S2：高危授权申请 3 端点（list/create/decision）。镜像既有注入范式。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });

spec.paths['/api/v1/access/grant-requests'] = {
  get: {
    tags: ['access'], operationId: 'listAccessGrantRequests', summary: '高危授权申请清单',
    'x-permission': 'access.role.assign', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'access.grant_request.viewed', 'x-audit-event-failure': 'access.grant_request.view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
  post: {
    tags: ['access'], operationId: 'createAccessGrantRequest', summary: '提交高危授权申请（转主管复核待批）',
    description: '④ S2：当 access.grant_approval.enabled 开，含 requires_approval 权限的装配不得直接落地，须经此申请→主管复核(permission_grant_l3)审批后由 decision 落地。',
    'x-permission': 'access.role.assign', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'access.grant_request.created', 'x-audit-event-failure': 'access.grant_request.create_failed',
    'x-auth-context': 'staff-token', requestBody: { $ref: '#/components/requestBodies/AccessGrantRequestCreateRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/access/grant-requests/{grant_request_id}/decision'] = {
  post: {
    tags: ['access'], operationId: 'decideAccessGrantRequest', summary: '审批高危授权申请（approved 落地 / rejected 驳回）',
    description: '④ S2：经既有主管复核 approve（发起人≠审批人 SoD、职级 rank、角色闸）；通过才落 user_roles/role_permissions。',
    'x-permission': 'governance.supervisor_review.approve', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'access.grant_request.decided', 'x-audit-event-failure': 'access.grant_request.decide_failed',
    'x-auth-context': 'staff-token',
    parameters: [{ name: 'grant_request_id', in: 'path', required: true, schema: { type: 'string', maxLength: 64 }, description: '授权申请 id。' }],
    requestBody: { $ref: '#/components/requestBodies/AccessGrantRequestDecisionRequest' }, responses: ok,
  },
};

spec.components.requestBodies.AccessGrantRequestCreateRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/AccessGrantRequestCreateRequest' } } },
};
spec.components.requestBodies.AccessGrantRequestDecisionRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/AccessGrantRequestDecisionRequest' } } },
};
spec.components.schemas.AccessGrantRequestCreateRequest = {
  type: 'object', description: '请求载荷 AccessGrantRequestCreateRequest（④ S2 高危授权申请）。', additionalProperties: false,
  required: ['request_type', 'role_id'],
  properties: {
    idempotency_key: idem,
    request_type: { type: 'string', description: '申请类型。', enum: ['user_role', 'role_permission'] },
    user_id: str('目标用户 id（request_type=user_role 必填）。', 64),
    role_id: str('目标角色 id。', 64),
    permission_code: str('目标权限码（request_type=role_permission 必填）。', 128),
    reason: str('申请理由。', 1000),
  },
};
spec.components.schemas.AccessGrantRequestDecisionRequest = {
  type: 'object', description: '请求载荷 AccessGrantRequestDecisionRequest（④ S2 授权审批）。', additionalProperties: false,
  required: ['decision'],
  properties: {
    idempotency_key: idem,
    decision: { type: 'string', description: '审批结论。', enum: ['approved', 'rejected'] },
    comment: str('审批意见。', 1000),
  },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ④S2: 2 paths(3 ops) + 2 requestBodies + 2 schemas');
