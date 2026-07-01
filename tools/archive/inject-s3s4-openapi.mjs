// 一次性注入 ③ 楼宇运营 S4：能耗抄表 4 端点（表 list/create + 读数 record/list）。镜像 s3s1..s3s3 注入范式。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const err = { $ref: '#/components/responses/Error' };
const ok = { '200': { $ref: '#/components/responses/Success' }, '400': err, '403': err, '404': err, '409': err, '500': err };
const web = ['web'];
const idem = { type: 'string', description: '操作幂等键。', maxLength: 128 };
const str = (d, max = 255) => ({ type: 'string', description: d, maxLength: max });

spec.paths['/api/v1/facility/energy/meters'] = {
  get: {
    tags: ['facility'], operationId: 'listEnergyMeters', summary: '能耗表台账清单',
    'x-permission': 'facility.energy.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.energy.meter_viewed', 'x-audit-event-failure': 'facility.energy.meter_view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
  post: {
    tags: ['facility'], operationId: 'createEnergyMeter', summary: '新建能耗表',
    description: '③ S4：电/水/气表建档。功能标志 facility.energy.enabled 默认关。',
    'x-permission': 'facility.energy.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.energy.meter_created', 'x-audit-event-failure': 'facility.energy.meter_create_failed',
    'x-auth-context': 'staff-token', requestBody: { $ref: '#/components/requestBodies/EnergyMeterRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/facility/energy/meters/{meter_id}/readings'] = {
  post: {
    tags: ['facility'], operationId: 'recordEnergyMeterReading', summary: '能耗抄表录入（系统算本期用量）',
    description: '③ S4：录入表底读数，系统按上期表底算 consumption=本期-上期；同表同期次唯一（幂等地基），读数单调不减。',
    'x-permission': 'facility.energy.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': true, 'x-audit-event-success': 'facility.energy.reading_recorded', 'x-audit-event-failure': 'facility.energy.reading_record_failed',
    'x-auth-context': 'staff-token',
    parameters: [{ name: 'meter_id', in: 'path', required: true, schema: { type: 'string', maxLength: 64 }, description: '能耗表 id。' }],
    requestBody: { $ref: '#/components/requestBodies/EnergyMeterReadingRequest' }, responses: ok,
  },
};
spec.paths['/api/v1/facility/energy/readings'] = {
  get: {
    tags: ['facility'], operationId: 'listEnergyMeterReadings', summary: '能耗抄表读数清单',
    'x-permission': 'facility.energy.manage', 'x-data-scope': 'platform', 'x-terminal-allowed': web,
    'x-idempotency-required': false, 'x-audit-event-success': 'facility.energy.reading_viewed', 'x-audit-event-failure': 'facility.energy.reading_view_failed',
    'x-auth-context': 'staff-token', responses: { '200': { $ref: '#/components/responses/Page' }, '403': err, '500': err },
  },
};

spec.components.requestBodies.EnergyMeterRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/EnergyMeterRequest' } } },
};
spec.components.requestBodies.EnergyMeterReadingRequest = {
  required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/EnergyMeterReadingRequest' } } },
};
spec.components.schemas.EnergyMeterRequest = {
  type: 'object', description: '请求载荷 EnergyMeterRequest（③ S4 能耗表建档）。', additionalProperties: false,
  required: ['name', 'meter_type'],
  properties: {
    idempotency_key: idem, name: str('表名称。'),
    meter_type: { type: 'string', description: '表类型。', enum: ['electricity', 'water', 'gas'] },
    space_id: str('安装位置空间 id。', 64), unit: str('计量单位（空=kWh）。', 16), merchant_id: str('归属商户 id（公区表留空）。', 64),
  },
};
spec.components.schemas.EnergyMeterReadingRequest = {
  type: 'object', description: '请求载荷 EnergyMeterReadingRequest（③ S4 抄表录入）。', additionalProperties: false,
  required: ['period', 'reading_value'],
  properties: {
    idempotency_key: idem, period: str('抄表期次（如 2026-06）。', 16),
    reading_value: { type: 'number', description: '表底读数（单调不减）。', minimum: 0 },
    recorded_by: str('抄表人 id。', 64),
  },
};

writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ③S4: 3 paths(4 ops) + 2 requestBodies + 2 schemas');
