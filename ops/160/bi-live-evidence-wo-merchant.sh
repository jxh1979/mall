#!/usr/bin/env bash
# 清单#13 活证据:商户服务工单并入 BI 工单时效——
# 造数(超时关单/在办超时/已取消)→ ETL 快照出 mwo.* 三指标(无 mwo.response 诚实空缺)→
# 明细钻取 source=merchant 同口径(含超时单/不含已取消)→ 默认 source 不混入商户单 → 非法 source 拒。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
PG=postgresql://complex:${COMPLEX_DATABASE_PASSWORD:-complex_dev_pwd}@127.0.0.1:5432/complex

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"

echo "== 0. 清场旧证据数据(避免重放累积) =="
psql $PG -c "DELETE FROM merchant_service_work_orders WHERE id LIKE 'mwo_ev_%';" > /dev/null

echo "== 1. 造数:B超时关单(升级已标记) / C在办超时(实时兜底) / D已取消(不计) =="
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
DUE=$(date -u -d '-30 min' +%Y-%m-%dT%H:%M:%SZ)
CRT=$(date -u -d '-90 min' +%Y-%m-%dT%H:%M:%SZ)
psql $PG -c "
INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status, title, description,
        reported_by_user_id, reported_channel, evidence_attachment_ids, due_at, closed_at, created_at, updated_at, sla_breached)
VALUES
 ('mwo_ev_b_$STAMP','mch_ev','repair','high','closed','证据B超时关单','d','usr_ev','web','[]'::jsonb,
  '$DUE'::timestamptz,'$NOW'::timestamptz,'$CRT'::timestamptz,'$NOW'::timestamptz,true),
 ('mwo_ev_c_$STAMP','mch_ev','repair','medium','processing','证据C在办超时','d','usr_ev','web','[]'::jsonb,
  '$DUE'::timestamptz,NULL,'$CRT'::timestamptz,'$NOW'::timestamptz,false),
 ('mwo_ev_d_$STAMP','mch_ev','repair','low','cancelled','证据D已取消','d','usr_ev','web','[]'::jsonb,
  '$DUE'::timestamptz,NULL,'$CRT'::timestamptz,'$NOW'::timestamptz,false);
" > /dev/null
echo "seeded 3 orders (due=$DUE)"

echo "== 2. 等 ETL 快照产出 mwo.* 指标 =="
sleep 65
for i in $(seq 1 20); do
  OK=$(curl -s "$API/api/v1/analytics/dashboards/facility" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data'].get('metrics') or {}
def has(k): return (m.get(k) or {}).get('value') is not None
print('yes' if has('mwo.overdue_rate') and has('mwo.backlog') else 'no')
")
  [ "$OK" = "yes" ] && break
  sleep 15
done
curl -s "$API/api/v1/analytics/dashboards/facility" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
assert 'mwo.overdue_rate' in m and 'mwo.backlog' in m, sorted(k for k in m if k.startswith('mwo'))
assert float(m['mwo.backlog']['value']) >= 1, m['mwo.backlog']
assert float(m['mwo.overdue_rate']['value']) > 0, m['mwo.overdue_rate']
assert 'mwo.response_hours_p50' not in m, '商户单无响应节点,不应产出响应P50'
print('PASS: ETL snapshot has mwo.overdue_rate=%s mwo.backlog=%s (honest: no mwo.response P50)'
      % (m['mwo.overdue_rate']['value'], m['mwo.backlog']['value']))
"

echo "== 3. 明细钻取 source=merchant:含超时两单,不含已取消 =="
curl -s "$API/api/v1/analytics/work-order-sla/overdue-orders?source=merchant&limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
ids=[r['work_order_id'] for r in rows]
assert 'mwo_ev_b_$STAMP' in ids and 'mwo_ev_c_$STAMP' in ids, ids[:10]
assert 'mwo_ev_d_$STAMP' not in ids, 'cancelled 不应进超时明细'
b=[r for r in rows if r['work_order_id']=='mwo_ev_b_$STAMP'][0]
assert float(b['overdue_hours']) >= 0.4, b
print('PASS: merchant drilldown rows=%d, closed-late overdue_hours=%s' % (len(rows), b['overdue_hours']))
"

echo "== 4. 默认 source(设施单):不混入商户单 =="
curl -s "$API/api/v1/analytics/work-order-sla/overdue-orders?limit=200" -H "$AH" | python3 -c "
import sys,json
ids=[r['work_order_id'] for r in json.load(sys.stdin)['data']['items']]
assert not any(i.startswith('mwo_ev_') for i in ids), '设施明细混入商户单'
print('PASS: facility drilldown clean (%d rows, no merchant orders)' % len(ids))
"

echo "== 5. 非法 source 拒(诚实校验) =="
curl -s "$API/api/v1/analytics/work-order-sla/overdue-orders?source=bogus" -H "$AH" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False, str(d)[:200]
print('PASS: invalid source rejected')
"

echo
echo "ALL LIVE EVIDENCE PASSED (清单#13 商户服务工单并入工单时效)"
