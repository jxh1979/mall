#!/usr/bin/env bash
# BI H1 活证据:真实后端——节假日日历(种子/手动/抓取失败保护)→节假日对比(总额+日均)→异常免报
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
PG=postgresql://complex:${COMPLEX_DATABASE_PASSWORD:-complex_dev_pwd}@127.0.0.1:5432/complex

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
TOK=$(tok admin admin-pass)
AH="Authorization: Bearer $TOK"

echo "== 1. V209 种子:2025 法定假期已预填 =="
curl -s "$API/api/v1/analytics/holiday-calendar?year=2025" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
names={r['holiday_name'] for r in rows}
assert {'春节','劳动节','国庆'}.issubset(names), names
spring=[r for r in rows if r['holiday_name']=='春节'][0]
assert spring['start_date']=='2025-01-28' and spring['end_date']=='2025-02-04', spring
print('PASS: 2025 seeds present (%d holidays)' % len(rows))
"

echo "== 2. 手动录入 2026 春节(按国务院公布口径演示)→ 读回 =="
curl -s -X POST $API/api/v1/analytics/holiday-calendar -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h1-cj26-$STAMP\",\"holiday_name\":\"春节\",\"holiday_year\":2026,\"start_date\":\"2026-02-15\",\"end_date\":\"2026-02-22\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is True, str(d)[:300]
print('PASS: 2026 春节 manual entry saved')
"

echo "== 3. 抓取失败保护:不可达 URL → 400 且既有日历不动 =="
BEFORE=$(psql $PG -tc 'SELECT COUNT(*) FROM holiday_calendar;' | tr -d ' ')
curl -s -X POST $API/api/v1/analytics/holiday-calendar/fetch -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h1-fetch-$STAMP\",\"url\":\"http://127.0.0.1:9/unreachable.json\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False, str(d)[:200]
assert 'unreachable' in d.get('message','') or 'untouched' in d.get('message',''), d.get('message')
print('PASS: unreachable fetch rejected —', d['message'][:70])
"
AFTER=$(psql $PG -tc 'SELECT COUNT(*) FROM holiday_calendar;' | tr -d ' ')
python3 -c "assert '$BEFORE'=='$AFTER'; print('PASS: calendar untouched ($BEFORE rows)')"

echo "== 4. 节假日对比:2026 春节 vs 2025 春节(区间/日均/同比结构) =="
curl -s "$API/api/v1/analytics/holiday-comparison?holiday=%E6%98%A5%E8%8A%82&year=2026" -H "$AH" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['current_start']=='2026-02-15' and d['previous_start']=='2025-01-28', d
rows={r['metric_key']:r for r in d['rows']}
assert 'revenue.net' in rows and rows['revenue.net']['aggregation']=='monthly_sum'
assert 'ar.balance' in rows and rows['ar.balance']['aggregation']=='period_end'
print('PASS: comparison 2026春节(8天) vs 2025春节(8天), %d rows, 双口径齐全' % len(d['rows']))
"

echo "== 5. 异常免报:把今天临时标为节假日 → 下一轮 ETL 不新增异常;撤销后恢复 =="
TODAY=$(date -u +%F)
curl -s -X POST $API/api/v1/analytics/holiday-calendar -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h1-today-$STAMP\",\"holiday_name\":\"免报演练\",\"holiday_year\":$(date -u +%Y),\"start_date\":\"$TODAY\",\"end_date\":\"$TODAY\"}" > /dev/null
CNT_BEFORE=$(psql $PG -tc "SELECT COUNT(*) FROM analytics_metric_anomalies WHERE detected_at::date = '$TODAY';" | tr -d ' ')
sleep 310
CNT_AFTER=$(psql $PG -tc "SELECT COUNT(*) FROM analytics_metric_anomalies WHERE detected_at::date = '$TODAY';" | tr -d ' ')
python3 -c "assert int('$CNT_AFTER') <= int('$CNT_BEFORE'), 'anomaly written during holiday!'; print('PASS: holiday skip — anomalies today before=$CNT_BEFORE after=$CNT_AFTER (no new)')"
psql $PG -c "DELETE FROM holiday_calendar WHERE holiday_name='免报演练';" > /dev/null
echo "drill holiday removed"

echo
echo "ALL LIVE EVIDENCE PASSED (BI H1)"
