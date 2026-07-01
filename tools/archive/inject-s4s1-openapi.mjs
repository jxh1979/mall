// 一次性注入 ④ 集团治理 S1：角色装配 4 端点（派/解角色 + 角色权限增/删）。镜像既有注入范式；POST-only（沿用 revoke 约定）。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const pathParam = (name, desc) => ({ name, in: 'path', required: true, schema: { type: 'string', maxLength: 128 }, description: desc });

spec.paths['/api/v1/access/users/{user_id}/roles'] = {
  post: {
    tags: ['access'], operationId: 'assignUserRole', summary: '给用户装配角色',
    description: '④ S1：用户↔角色直接装配。派角色＝授该角色全部权限，过提权闸（不得超出授权人自身权限）+ 跨组织闸。',
    'x-permission': 'access.role.assign', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'access.user_role.assigned', 'x-audit-event-failure': 'access.user_role.assign_failed',
    'x-auth-context': 'staff-token',
    parameters: [pathParam('user_id', '用户 id。')],
    requestBody: { $ref: '#/components/requestBodies/AccessRoleAssignmentRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/access/users/{user_id}/roles/{role_id}/unassign'] = {
  post: {
    tags: ['access'], operationId: 'unassignUserRole', summary: '解绑用户角色',
    'x-permission': 'access.role.assign', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'access.user_role.unassigned', 'x-audit-event-failure': 'access.user_role.unassign_failed',
    'x-auth-context': 'staff-token',
    parameters: [pathParam('user_id', '用户 id。'), pathParam('role_id', '角色 id。')],
    responses: { '200': { $ref: '#/components/responses/Success' }, '403': err, '404': err, '500': err },
  },
};
spec.paths['/api/v1/access/roles/{role_id}/permissions'] = {
  post: {
    tags: ['access'], operationId: 'addRolePermission', summary: '给角色加一条权限（运行时）',
    description: '④ S1：角色权限运行时增删，过提权闸 + 自有角色闸。',
    'x-permission': 'access.role.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'access.role_permission.added', 'x-audit-event-failure': 'access.role_permission.add_failed',
    'x-auth-context': 'staff-token',
    parameters: [pathParam('role_id', '角色 id。')],
    requestBody: { $ref: '#/components/requestBodies/RolePermissionRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/access/roles/{role_id}/permissions/{permission_code}/remove'] = {
  post: {
    tags: ['access'], operationId: 'removeRolePermission', summary: '从角色移除一条权限',
    'x-permission': 'access.role.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'access.role_permission.removed', 'x-audit-event-failure': 'access.role_permission.remove_failed',
    'x-auth-context': 'staff-token',
    parameters: [pathParam('role_id', '角色 id。'), pathParam('permission_code', '权限码。')],
    responses: { '200': { $ref: '#/components/responses/Success' }, '403': err, '404': err, '500': err },
  },
};

spec.components.requestBodies.AccessRoleAssignmentRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/AccessRoleAssignmentRequest' } } },
};
spec.components.requestBodies.RolePermissionRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/RolePermissionRequest' } } },
};
spec.components.schemas.AccessRoleAssignmentRequest = {
  type: 'object', description: '请求载荷 AccessRoleAssignmentRequest（④ S1 用户↔角色装配）。', additionalProperties: false,
  required: ['role_id'],
  properties: { idempotency_key: idem, role_id: { type: 'string', description: '角色 id。', maxLength: 64 } },
};
spec.components.schemas.RolePermissionRequest = {
  type: 'object', description: '请求载荷 RolePermissionRequest（④ S1 角色权限增删）。', additionalProperties: false,
  required: ['permission_code'],
  properties: { idempotency_key: idem, permission_code: { type: 'string', description: '权限码。', maxLength: 128 } },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ④S1: 4 paths(4 ops) + 2 requestBodies + 2 schemas');
