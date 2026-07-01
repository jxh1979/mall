// 条目2 注入：外部协作机构与自助下属门户 5 端点 + 2 requestBody。用法：node inject-partner-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2] || 'docs/openapi/openapi-p0.json';
const spec = JSON.parse(readFileSync(path, 'utf8'));
spec.components ??= {}; spec.components.schemas ??= {}; spec.components.requestBodies ??= {};
const err = { $ref: '#/components/responses/Error' };
const pageR = { '200': { $ref: '#/components/responses/Page' }, '401': err, '403': err, '500': err };
const okR = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };

spec.components.schemas.ExternalPartnerOrgRequest = {
  type: 'object', additionalProperties: false,
  description: '平台管理员录入/修改外部协作机构（含下属上限与账号时效）。',
  required: ['idempotency_key', 'org_name', 'org_category'],
  properties: {
    idempotency_key: { type: 'string', maxLength: 128, description: '操作幂等键。' },
    project_id: { type: 'string', maxLength: 64, description: '综合体归属。' },
    org_name: { type: 'string', maxLength: 120, description: '机构名称。' },
    org_category: { type: 'string', maxLength: 32, description: 'facility_maintenance/security_cleaning/construction/external_audit/other。' },
    linked_supplier_id: { type: 'string', maxLength: 64, description: '可选：指向供货供应商记录。' },
    max_subordinates: { type: 'integer', minimum: 0, description: '下属账号数上限。' },
    valid_until: { type: 'string', format: 'date-time', description: '账号有效期（随合同；空=长期）。' },
    status: { type: 'string', maxLength: 32, description: 'active/suspended/expired。' },
  },
};
spec.components.schemas.PartnerSubordinateRequest = {
  type: 'object', additionalProperties: false,
  description: '机构管理员自助开通下属账号。',
  required: ['idempotency_key', 'username'],
  properties: {
    idempotency_key: { type: 'string', maxLength: 128, description: '操作幂等键。' },
    username: { type: 'string', maxLength: 64, description: '下属账号登录名。' },
  },
};
spec.components.requestBodies.ExternalPartnerOrgRequest = { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/ExternalPartnerOrgRequest' } } } };
spec.components.requestBodies.PartnerSubordinateRequest = { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/PartnerSubordinateRequest' } } } };

const base = (e) => ({ tags: ['partner'], 'x-permission': e.perm, 'x-data-scope': e.scope, 'x-terminal-allowed': ['web'],
  'x-idempotency-required': !!e.idem, 'x-audit-event-success': e.ok, 'x-audit-event-failure': e.fail, 'x-auth-context': 'staff-token',
  operationId: e.id, summary: e.summary, description: e.desc });

spec.paths['/api/v1/partner/orgs'] = {
  get: { ...base({ id: 'listPartnerOrgs', perm: 'partner.org.view', scope: 'platform', ok: 'partner_org.viewed', fail: 'partner_org.view_failed', summary: '外部协作机构列表', desc: '平台管理员查看外部协作机构（含下属上限/时效）。' }), responses: pageR },
  post: { ...base({ id: 'createPartnerOrg', perm: 'partner.org.manage', scope: 'platform', idem: true, ok: 'partner_org.updated', fail: 'partner_org.update_failed', summary: '录入/修改外部协作机构', desc: '平台管理员录入或修改外部协作机构、设下属上限与账号时效。' }), requestBody: { $ref: '#/components/requestBodies/ExternalPartnerOrgRequest' }, responses: okR },
};
spec.paths['/api/v1/partner/subordinates'] = {
  get: { ...base({ id: 'listPartnerSubordinates', perm: 'partner.subordinate.view', scope: 'self', ok: 'partner_subordinate.viewed', fail: 'partner_subordinate.view_failed', summary: '本机构下属账号列表', desc: '机构管理员查看本机构下属账号及状态。' }), responses: pageR },
  post: { ...base({ id: 'createPartnerSubordinate', perm: 'partner.subordinate.manage', scope: 'self', idem: true, ok: 'partner_subordinate.created', fail: 'partner_subordinate.create_failed', summary: '自助开通下属账号', desc: '机构管理员在下属上限内自助开通下属账号；超上限/机构停用或过期则拒。' }), requestBody: { $ref: '#/components/requestBodies/PartnerSubordinateRequest' }, responses: okR },
};
spec.paths['/api/v1/partner/subordinates/{user_id}/disable'] = {
  post: { ...base({ id: 'disablePartnerSubordinate', perm: 'partner.subordinate.manage', scope: 'self', idem: false, ok: 'partner_subordinate.disabled', fail: 'partner_subordinate.disable_failed', summary: '停用下属账号', desc: '机构管理员停用本机构下属账号；仅限本机构。' }),
    parameters: [{ name: 'user_id', in: 'path', required: true, description: '下属账号 ID。', schema: { type: 'string', maxLength: 64 } }], responses: okR },
};
writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected 5 partner ops + 2 requestBodies');
