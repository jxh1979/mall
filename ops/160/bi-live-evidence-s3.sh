#!/usr/bin/env bash
# BI S3 活证据:真实后端造铺位+合同全流程(起草→并行会签→总经理→激活) → ETL 快照 → 出租率/空置明细/到期预警同口径
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

TODAY=$(date -u +%F)
EXPIRY=$(date -u -d '+45 days' +%F)

echo "== 1. 造铺位:商铺A(100㎡,保持空置)/商铺B(50㎡,将出租)/车位P(15㎡,单列) =="
SPA=$(curl -s -X POST $API/api/v1/platform/spaces -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-spa-$STAMP\",\"space_name\":\"S3空置商铺$STAMP\",\"space_type\":\"shop\",\"floor_no\":\"2F\",\"area_square_meter\":100}" | jid)
SPB=$(curl -s -X POST $API/api/v1/platform/spaces -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-spb-$STAMP\",\"space_name\":\"S3出租商铺$STAMP\",\"space_type\":\"shop\",\"floor_no\":\"1F\",\"area_square_meter\":50}" | jid)
SPP=$(curl -s -X POST $API/api/v1/platform/spaces -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-spp-$STAMP\",\"space_name\":\"S3车位$STAMP\",\"space_type\":\"parking\",\"area_square_meter\":15}" | jid)
echo "A=$SPA B=$SPB P=$SPP"

echo "== 2. 造商户 + 合同全流程:起草(绑B,45天后到期)→提交→业务/财务/法务并行会签→总经理→激活生效 =="
ORG=$(curl -s -X POST $API/api/v1/platform/business-entities -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-org-$STAMP\",\"entity_name\":\"S3主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"s3-$STAMP\",\"legal_representative\":\"S3\",\"bank_account_ref\":\"vault://complex/s3/$STAMP\",\"status\":\"active\",\"operator_note\":\"s3\",\"evidence_attachment_ids\":[\"att-s3-$STAMP\"]}" | jid)
M=$(curl -s -X POST $API/api/v1/platform/merchants -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-m-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"S3租户$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"S3\",\"contact_phone\":\"18800000062\",\"status\":\"active\",\"operator_note\":\"s3\",\"evidence_attachment_ids\":[\"att-s3-$STAMP\"]}" | jid)
C=$(curl -s -X POST $API/api/v1/contracts -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-c-$STAMP\",\"merchant_id\":\"$M\",\"contract_subject_id\":\"$ORG\",\"settlement_subject_id\":\"$ORG\",\"effective_date\":\"$TODAY\",\"expiry_date\":\"$EXPIRY\",\"rent_terms\":{\"baseRentCent\":100000},\"commission_mode\":\"fixed_ratio\",\"energy_fee_basis\":\"by_area\",\"deposit_cent\":0,\"business_scope\":\"S3活证据\",\"space_ids\":[\"$SPB\"]}" | jid)
curl -s -X POST $API/api/v1/contracts/$C/submit -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-sub-$STAMP\"}" > /dev/null
for CODE in business finance legal gm; do
  curl -s -X POST $API/api/v1/contracts/$C/approve -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-ap-$CODE-$STAMP\",\"approver_permission_code\":\"contract.approve.$CODE\",\"decision\":\"approve\",\"comment\":\"s3\"}" > /dev/null
done
ACT=$(curl -s -X POST $API/api/v1/contracts/$C/activate -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s3-act-$STAMP\",\"space_ids\":[\"$SPB\"]}")
echo "$ACT" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success'), d
assert d['data']['lifecycle_status']=='effective', d['data'].get('lifecycle_status')
print('contract effective:', d['data']['id'])"

echo "== 3. 等 ETL 定时刷新(每5分钟)把出租率写入快照 =="
for i in $(seq 1 26); do
  OK=$(curl -s "$API/api/v1/analytics/dashboards/leasing" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
leased=m.get('occupancy.leased_area',{}).get('value')
print(1 if leased is not None and float(leased)>=50 else 0)")
  [ "$OK" = "1" ] && break
  sleep 15
done
[ "$OK" = "1" ] || { echo "ETL 未在时限内产出已租面积"; exit 1; }

echo "== 4. 快照口径断言:rate=leased/rentable、车位单列、90天到期计数含本合同 =="
curl -s "$API/api/v1/analytics/dashboards/leasing" -H "$AH" | python3 -c "
import sys,json
m=json.load(sys.stdin)['data']['metrics']
def v(k):
    x=m.get(k,{}).get('value'); return None if x is None else float(x)
rentable=v('occupancy.rentable_area'); leased=v('occupancy.leased_area'); rate=v('occupancy.rate')
assert rentable is not None and rentable>=150, rentable   # A100+B50 进分母
assert leased is not None and leased>=50, leased          # B 已租
assert rate is not None and abs(rate-leased/rentable)<0.0001, (rate, leased, rentable)
exp=v('contract.expiring_90d')
assert exp is not None and exp>=1, exp                    # 45天后到期的合同进 90 天预警
print(f'PASS: rentable={rentable} leased={leased} rate={rate}(=leased/rentable) expiring_90d={exp}')
"

echo "== 5. 空置明细与快照同口径:A 在列(空置)、B 不在列(已租)、车位不混入 =="
curl -s "$API/api/v1/analytics/occupancy/vacant-spaces?limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
ids=[r['space_id'] for r in rows]
assert '$SPA' in ids, 'A 空置铺位缺席'
assert '$SPB' not in ids, 'B 已租不应在空置明细'
assert '$SPP' not in ids, '车位不应混入铺位空置明细'
a=[r for r in rows if r['space_id']=='$SPA'][0]
assert float(a['area_sqm'])==100 and a['rental_status'] in ('vacant','moved_out'), a
print('PASS: vacant list has A(100sqm) only; B leased & parking excluded — drilldown==snapshot口径')
"

echo "== 6. 到期预警明细:本合同在列,剩余45天,绑定面积=50㎡ =="
curl -s "$API/api/v1/analytics/occupancy/expiring-contracts?window_days=90&limit=200" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['contract_id']=='$C']
assert mine, 'contract missing in expiring list'
r=mine[0]
assert int(r['days_left'])==45, r
assert float(r['bound_area'])==50, r
assert r['merchant_id']=='$M', r
print('PASS: expiring contract days_left=45 bound_area=50 merchant matches')
"

echo
echo "ALL LIVE EVIDENCE PASSED (BI S3)"
