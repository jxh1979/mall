#!/usr/bin/env bash
# BI M1+M2 活证据:真实后端——ETL 月度滚动快照(SUM/期末恒等式)→对比端点→分权导出+审计留痕
set -euo pipefail
API=http://127.0.0.1:8080
J='Content-Type: application/json'
PG=postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
TOK=$(tok admin admin-pass)
AH="Authorization: Bearer $TOK"
MONTH=$(date -u +%Y-%m)
MONTH_FIRST=$(date -u +%Y-%m-01)

echo "== 1. 等 ETL 定时刷新(每5分钟)生成当月 monthly 快照行 =="
for i in $(seq 1 26); do
  N=$(psql $PG -tc "SELECT COUNT(*) FROM analytics_business_metric_snapshots WHERE period_type='monthly' AND business_date='$MONTH_FIRST';" | tr -d ' ')
  [ "$N" -gt 0 ] && break
  sleep 15
done
[ "$N" -gt 0 ] || { echo "ETL 未在时限内产出月度快照"; exit 1; }
echo "monthly rows: $N"

echo "== 2. 口径恒等式(真库):月累计键 = Σ当月daily;期末键 = 最新经营日 daily 值 =="
psql $PG -tc "
WITH m AS (SELECT metric_key, actual_value FROM analytics_business_metric_snapshots
            WHERE period_type='monthly' AND business_date='$MONTH_FIRST' AND segment_type='platform'),
 dsum AS (SELECT metric_key, SUM(actual_value) v FROM analytics_business_metric_snapshots
            WHERE period_type='daily' AND segment_type='platform' AND business_date >= '$MONTH_FIRST'
              AND metric_key='revenue.net' GROUP BY 1),
 dlast AS (SELECT DISTINCT ON (metric_key) metric_key, actual_value v FROM analytics_business_metric_snapshots
            WHERE period_type='daily' AND segment_type='platform' AND business_date >= '$MONTH_FIRST'
              AND metric_key='ar.balance' ORDER BY metric_key, business_date DESC)
SELECT
  (SELECT CASE WHEN m.actual_value = dsum.v THEN 'SUM_OK' ELSE 'SUM_MISMATCH '||m.actual_value||' vs '||dsum.v END
     FROM m JOIN dsum ON dsum.metric_key=m.metric_key WHERE m.metric_key='revenue.net'),
  (SELECT CASE WHEN m.actual_value = dlast.v THEN 'LAST_OK' ELSE 'LAST_MISMATCH '||m.actual_value||' vs '||dlast.v END
     FROM m JOIN dlast ON dlast.metric_key=m.metric_key WHERE m.metric_key='ar.balance');" | python3 -c "
import sys
line = sys.stdin.read().strip()
assert 'SUM_OK' in line and 'LAST_OK' in line, line
print('PASS: monthly(revenue.net)=SUM(daily), monthly(ar.balance)=period-end daily —', line)
"

echo "== 3. 对比端点:本月值非空;历史月缺行 → 环比/同比标 null(积累中,不伪造) =="
curl -s "$API/api/v1/analytics/monthly-comparison?month=$MONTH" -H "$AH" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['month']=='$MONTH', d['month']
rows={r['metric_key']:r for r in d['rows']}
bal=rows['ar.balance']
assert bal['current'] is not None, bal
print('PASS: comparison month=%s ar.balance current=%s mom_pct=%s(缺上月→null即积累中)' % (d['month'], bal['current'], bal['mom_pct']))
"

echo "== 4. 分权导出:admin(有 analytics.report.export)导出成功 + 审计事件落库 =="
curl -s "$API/api/v1/analytics/reports/monthly-export?month=$MONTH" -H "$AH" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is True, str(d)[:300]
assert len(d['data']['rows'])>=10, len(d['data']['rows'])
print('PASS: export returned %d rows' % len(d['data']['rows']))
"
EV=$(psql $PG -tc "SELECT COUNT(*) FROM audit_events WHERE event_name='analytics.monthly_report.exported';" | tr -d ' ')
python3 -c "assert int('$EV')>=1, 'export audit event missing'; print('PASS: export audit event rows=$EV')"

echo
echo "ALL LIVE EVIDENCE PASSED (BI M1+M2)"
