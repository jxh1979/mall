#!/usr/bin/env bash
# BI S2 活证据:真实后端造欠费 → ETL 快照刷新 → 账龄桶/欠费总额/明细钻取同口径
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
    sys.stderr.write("RESPONSE: "+raw[:300]+"\n"); raise SystemExit(1)'; }

D45=$(date -u -d '45 days ago' +%F)
D100=$(date -u -d '100 days ago' +%F)

echo "== 1. 造欠费:商户 M,两笔逾期应收(45天前 500 元 / 100天前 300 元) =="
ORG=$(curl -s -X POST $API/api/v1/platform/business-entities -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s2-org-$STAMP\",\"entity_name\":\"S2主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"s2-$STAMP\",\"legal_representative\":\"S2\",\"bank_account_ref\":\"vault://complex/s2/$STAMP\",\"status\":\"active\",\"operator_note\":\"s2\",\"evidence_attachment_ids\":[\"att-s2-$STAMP\"]}" | jid)
M=$(curl -s -X POST $API/api/v1/platform/merchants -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s2-m-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"S2欠费商户$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"S2\",\"contact_phone\":\"18800000061\",\"status\":\"active\",\"operator_note\":\"s2\",\"evidence_attachment_ids\":[\"att-s2-$STAMP\"]}" | jid)
AR1=$(curl -s -X POST $API/api/v1/finance/ar-ap-items -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s2-ar1-$STAMP\",\"direction\":\"receivable\",\"counterparty_id\":\"$M\",\"merchant_id\":\"$M\",\"source_type\":\"manual\",\"source_id\":\"s2-src1-$STAMP\",\"amount_cent\":50000,\"due_date\":\"$D45\",\"accounting_period\":\"$(date -u -d '45 days ago' +%Y-%m)\",\"operator_note\":\"s2\",\"evidence_attachment_ids\":[\"att-s2-$STAMP\"]}" | jid)
AR2=$(curl -s -X POST $API/api/v1/finance/ar-ap-items -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s2-ar2-$STAMP\",\"direction\":\"receivable\",\"counterparty_id\":\"$M\",\"merchant_id\":\"$M\",\"source_type\":\"manual\",\"source_id\":\"s2-src2-$STAMP\",\"amount_cent\":30000,\"due_date\":\"$D100\",\"accounting_period\":\"$(date -u -d '100 days ago' +%Y-%m)\",\"operator_note\":\"s2\",\"evidence_attachment_ids\":[\"att-s2-$STAMP\"]}" | jid)
echo "M=$M AR45=$AR1 AR100=$AR2"

echo "== 2. 等 ETL 定时刷新(每5分钟)把新欠费写入快照 =="
for i in $(seq 1 26); do
  VAL=$(curl -s "$API/api/v1/analytics/dashboards/finance" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
v=m.get('ar.aging_31_60',{}).get('value')
print(v if v is not None else 0)")
  python3 -c "import sys; sys.exit(0 if float('$VAL')>=50000 else 1)" && break
  sleep 15
done
echo "aging_31_60 platform value: $VAL"

echo "== 3. 快照口径断言:桶含本次欠费,Σ桶=欠费总额 =="
curl -s "$API/api/v1/analytics/dashboards/finance" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
def v(k):
    x=m.get(k,{}).get('value'); return 0 if x is None else float(x)
buckets=v('ar.aging_0_30')+v('ar.aging_31_60')+v('ar.aging_61_90')+v('ar.aging_90p')
balance=v('ar.balance')
assert v('ar.aging_31_60')>=50000, m.get('ar.aging_31_60')
assert v('ar.aging_90p')>=30000, m.get('ar.aging_90p')
assert abs(buckets-balance)<0.01, f'sum(buckets)={buckets} != balance={balance}'
print(f'PASS: buckets sum {buckets} == balance {balance}; 31-60>=500元 90p>=300元 included')
"

echo "== 4. 明细钻取与快照同口径:本商户 outstanding=800元、桶=90+(按最老应收日) =="
curl -s "$API/api/v1/analytics/finance-receivables/debtors?limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['merchant_id']=='$M']
assert mine, 'merchant missing in debtors'
r=mine[0]
assert float(r['outstanding_cent'])==80000, r
assert r['bucket']=='a90p', r
assert r['oldest_due_date']=='$D100', r
print('PASS: debtor row outstanding=80000, bucket=a90p, oldest_due matches; drilldown==snapshot口径')
"

echo "== 5. 桶过滤钻取 =="
curl -s "$API/api/v1/analytics/finance-receivables/debtors?bucket=a0_30&limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert all(r['merchant_id']!='$M' for r in rows), 'bucket filter leaked'
print('PASS: bucket filter excludes 90p merchant from a0_30')
"

echo
echo "ALL LIVE EVIDENCE PASSED (BI S2)"
