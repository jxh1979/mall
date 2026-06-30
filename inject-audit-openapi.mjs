// 条目4 注入：跨对象操作留痕 查询/导出 2 个 GET op。用法：node inject-audit-openapi.mjs docs/openapi/openapi-p0.json
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2] || 'docs/openapi/openapi-p0.json';
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const pageResp = { '200': { $ref: '#/components/responses/Page' }, '401': err, '403': err, '500': err };
const q = (name, d, schema = { type: 'string', maxLength: 128 }) => ({ name, in: 'query', required: false, description: d, schema });
const filters = [
  q('business_type', '按业务对象类型过滤（如 ticket_order、contract）'),
  q('business_id', '按具体业务对象 ID 过滤'),
  q('actor_id', '按操作人过滤'),
  q('event_name', '按事件名过滤（如 ticket_refund.approved）'),
  q('from', '起始时间（含），ISO-8601 date-time', { type: 'string', format: 'date-time' }),
  q('to', '结束时间（含），ISO-8601 date-time', { type: 'string', format: 'date-time' }),
  q('limit', '最多返回条数（上限 1000）', { type: 'integer', minimum: 1, maximum: 1000 }),
];
const baseOp = (extra) => ({
  tags: ['audit'],
  'x-permission': extra.perm,
  'x-data-scope': 'platform',
  'x-terminal-allowed': ['web'],
  'x-idempotency-required': false,
  'x-audit-event-success': extra.evOk,
  'x-audit-event-failure': extra.evFail,
  'x-auth-context': 'staff-token',
  operationId: extra.id,
  summary: extra.summary,
  description: extra.desc,
  parameters: filters,
  responses: pageResp,
});
spec.paths['/api/v1/audit/operation-events'] = { get: baseOp({
  id: 'queryAuditOperationEvents', perm: 'audit.operation_log.view',
  summary: '跨对象查询操作留痕',
  desc: '审计只读角色按业务对象/操作人/事件名/时间范围跨对象查询 audit_events 操作日志（与 evidence/events 对象时间线互补，解决跨对象批量查询入口）。',
  evOk: 'audit.operation_log.viewed', evFail: 'audit.operation_log.view_failed' }) };
spec.paths['/api/v1/audit/operation-events/export'] = { get: baseOp({
  id: 'exportAuditOperationEvents', perm: 'audit.operation_log.export',
  summary: '导出操作留痕（敏感）',
  desc: '导出全量操作留痕，敏感动作；audit.operation_log.export 标 requires_approval，在授予层经审批放行后方可使用。',
  evOk: 'audit.operation_log.exported', evFail: 'audit.operation_log.export_failed' }) };
writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected 2 audit operation-events ops');
