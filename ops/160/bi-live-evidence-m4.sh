#!/usr/bin/env bash
# BI M4 活证据:真实后端——ETL 重算商户经营汇总(每商户一行,收入与看板同源;演示行已清)
set -euo pipefail
API=http://127.0.0.1:8080
J='Content-Type: application/json'
PG=postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
TOK=$(tok admin admin-pass)
AH="Authorization: Bearer $TOK"

echo "== 1. 演示种子已清(merchant_p0/health/staff 冒真数据不再存在) =="
psql $PG -tc "SELECT
  (SELECT COUNT(*) FROM analytics_merchant_summaries WHERE merchant_id='merchant_p0'),
  (SELECT COUNT(*) FROM analytics_merchant_health_scores),
  (SELECT COUNT(*) FROM analytics_staff_service_scores);" | python3 -c "
import sys
parts=[p.strip() for p in sys.stdin.read().split('|')]
assert parts==['0','0','0'], parts
print('PASS: demo seeds gone (summaries demo/health/staff all 0)')
"

echo "== 2. 等 ETL 定时刷新(每5分钟)回填真实商户汇总 =="
for i in $(seq 1 26); do
  N=$(psql $PG -tc "SELECT COUNT(*) FROM analytics_merchant_summaries;" | tr -d ' ')
  [ "$N" -gt 0 ] && break
  sleep 15
done
[ "$N" -gt 0 ] || { echo "ETL 未回填商户汇总"; exit 1; }
echo "summaries rows: $N"

echo "== 3. 口径恒等式(真库):汇总收入 = 当日商户维净收入快照(与看板同源) =="
psql $PG -tc "
SELECT COUNT(*) FROM analytics_merchant_summaries s
 JOIN analytics_business_metric_snapshots snap
   ON snap.segment_id = s.merchant_id AND snap.metric_key='revenue.net'
  AND snap.period_type='daily' AND snap.segment_type='merchant'
  AND snap.business_date = s.business_date
WHERE s.revenue_amount_cents <> GREATEST(snap.actual_value, 0)::bigint;" | python3 -c "
import sys
v=sys.stdin.read().strip()
assert v=='0', 'mismatch rows: '+v
print('PASS: every summary revenue equals same-day merchant revenue snapshot')
"

echo "== 4. 读端点返回真管道行(含商户名与结算状态) =="
curl -s "$API/api/v1/analytics/merchants?limit=200" -H "$AH" | python3 -c "
import sys,json
d=json.load(sys.stdin)
rows=d['data']['items'] if d.get('data') else []
assert len(rows)>0, 'no summary rows from endpoint'
r=rows[0]
assert r.get('merchant_name'), r
assert r.get('settlement_status') is not None, r
assert not any(x['merchant_id']=='merchant_p0' for x in rows), 'demo row leaked'
print('PASS: endpoint returns %d real pipeline rows, no demo row' % len(rows))
"

echo
echo "ALL LIVE EVIDENCE PASSED (BI M4)"
