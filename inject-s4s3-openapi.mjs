// 一次性注入 ④ 集团治理 S3：给既有 group-overview 加 groupBy 查询参（project|entity）。响应沿用 Page（泛型，不改 schema）。
import { readFileSync, writeFileSync } from 'node:fs';
const path = process.argv[2];
const spec = JSON.parse(readFileSync(path, 'utf8'));
const op = spec.paths['/api/v1/analytics/group-overview'].get;
op.parameters = (op.parameters || []).concat([{
  name: 'groupBy', in: 'query', required: false,
  schema: { type: 'string', enum: ['project', 'entity'] },
  description: '汇总维度：project(默认)按项目 / entity 按法律主体下钻；响应含集团均值与低于均值红点标记（④ S3 对标）。',
}]);
writeFileSync(path, JSON.stringify(spec, null, 2) + '\n');
console.log('injected ④S3: group-overview +groupBy query param');
