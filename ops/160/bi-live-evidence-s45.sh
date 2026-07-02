#!/usr/bin/env bash
# BI S4+S5 活证据:真实后端 SLA策略→工单流转→超时 / 表计→倒走读数→异常;ETL 快照与明细同口径
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
TOK=$(tok admin admin-pass)
AH="Authorization: Bearer $TOK"
jid() { python3 -c 'import sys,json
raw=sys.stdin.read()
try:
    print(json.loads(raw)["data"]["id"])
except Exception:
    sys.stderr.write("RESPONSE: "+raw[:400]+"\n"); raise SystemExit(1)'; }
ok() { python3 -c 'import sys,json
raw=sys.stdin.read()
d=json.loads(raw)
assert d.get("success"), raw[:400]
print("ok")'; }

PERIOD_1=$(date -u +%Y-%m)
PERIOD_2=$(date -u -d '+1 month' +%Y-%m)

echo "== 1. S4:配置 repair/urgent SLA=1分钟(管理员真实配置链路)→ 创建工单 → 派单 → 开始处理(响应) =="
curl -s -X POST $API/api/v1/facility/work-orders/policies -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-pol-$STAMP\",\"work_order_type\":\"repair\",\"priority\":\"urgent\",\"response_minutes\":1,\"handle_minutes\":1,\"requires_acceptance\":false,\"acceptance_by\":\"supervisor\",\"active\":true}" | ok > /dev/null
WO=$(curl -s -X POST $API/api/v1/facility/work-orders -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-wo-$STAMP\",\"work_order_type\":\"repair\",\"priority\":\"urgent\",\"title\":\"S45超时演练工单$STAMP\",\"description\":\"BI S4 活证据\",\"source\":\"platform\"}" | jid)
curl -s -X POST $API/api/v1/facility/work-orders/$WO/assign -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-as-$STAMP\",\"assignee_id\":\"staff-s45\"}" | ok > /dev/null
curl -s -X POST $API/api/v1/facility/work-orders/$WO/start -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-st-$STAMP\"}" | ok > /dev/null
echo "WO=$WO (handle_due=创建+1分钟,保持处理中)"

echo "== 2. S5:建电表 → 首期读数100 → 次期读数50(倒走,后端自动判 reverse) =="
MT=$(curl -s -X POST $API/api/v1/energy/meters -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-mt-$STAMP\",\"name\":\"S45活证据电表$STAMP\",\"meter_type\":\"electricity\",\"gather_object\":\"public_area\",\"read_mode\":\"manual\"}" | jid)
curl -s -X POST $API/api/v1/energy/readings -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-rd1-$STAMP\",\"meter_id\":\"$MT\",\"period\":\"$PERIOD_1\",\"reading_value\":100,\"read_mode\":\"manual\"}" | ok > /dev/null
RD2=$(curl -s -X POST $API/api/v1/energy/readings -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s45-rd2-$STAMP\",\"meter_id\":\"$MT\",\"period\":\"$PERIOD_2\",\"reading_value\":50,\"read_mode\":\"manual\"}" | jid)
echo "MT=$MT RD2=$RD2"

echo "== 3. 等 65 秒让工单处理时限(1分钟)过期 =="
sleep 65

echo "== 4. S4 明细钻取:该单在超时明细(未完成且时限已过),含已超时小时数 =="
curl -s "$API/api/v1/analytics/work-order-sla/overdue-orders?limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['work_order_id']=='$WO']
assert mine, 'work order missing in overdue drilldown'
r=mine[0]
assert r['completed_at'] is None, r
assert float(r['overdue_hours'])>=0, r
print('PASS: overdue drilldown has WO (uncompleted, past due), overdue_hours=%s' % r['overdue_hours'])
"

echo "== 5. S5 明细钻取:倒走读数在异常明细,原因=reverse =="
curl -s "$API/api/v1/analytics/energy/abnormal-readings?limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['reading_id']=='$RD2']
assert mine, 'abnormal reading missing in drilldown'
r=mine[0]
assert r['abnormal_reason']=='reverse', r
assert r['meter_type']=='electricity', r
print('PASS: abnormal drilldown has reverse reading on electricity meter')
"

echo "== 6. 等 ETL 定时刷新(每5分钟)写入快照 =="
for i in $(seq 1 26); do
  OK=$(curl -s "$API/api/v1/analytics/dashboards/facility" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
rate=m.get('wo.overdue_rate',{}).get('value')
print(1 if rate is not None and float(rate)>0 else 0)")
  [ "$OK" = "1" ] && break
  sleep 15
done
[ "$OK" = "1" ] || { echo "ETL 未在时限内产出超时率"; exit 1; }

echo "== 7. S4 快照口径断言:超时率>0、响应P50出现(start=响应落窗)、积压>=1 =="
curl -s "$API/api/v1/analytics/dashboards/facility" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
def v(k):
    x=m.get(k,{}).get('value'); return None if x is None else float(x)
assert v('wo.overdue_rate') is not None and 0 < v('wo.overdue_rate') <= 1, m.get('wo.overdue_rate')
assert v('wo.response_hours_p50') is not None and v('wo.response_hours_p50') >= 0, m.get('wo.response_hours_p50')
assert v('wo.backlog') is not None and v('wo.backlog') >= 1, m.get('wo.backlog')
print(f\"PASS: overdue_rate={v('wo.overdue_rate')} response_p50={v('wo.response_hours_p50')}h backlog={v('wo.backlog')}\")
"

echo "== 8. S5 快照口径断言:当日电量合计出现、异常计数>=1 =="
curl -s "$API/api/v1/analytics/dashboards/energy" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
def v(k):
    x=m.get(k,{}).get('value'); return None if x is None else float(x)
assert v('energy.total_electricity') is not None, 'electricity total missing'
assert v('energy.abnormal_count') is not None and v('energy.abnormal_count') >= 1, m.get('energy.abnormal_count')
print(f\"PASS: total_electricity={v('energy.total_electricity')} abnormal_count={v('energy.abnormal_count')}\")
"

echo
echo "ALL LIVE EVIDENCE PASSED (BI S4+S5)"
