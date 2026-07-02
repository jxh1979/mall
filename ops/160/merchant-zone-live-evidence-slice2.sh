#!/usr/bin/env bash
# 商户专区切片2 活证据:商户账号自助提交工单强制落本商户 + 只见本商户工单
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
PSQL="postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex"

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
ADMIN_TOK=$(tok p0_smoke_admin "$P0P")
AH="Authorization: Bearer $ADMIN_TOK"
jid() { python3 -c 'import sys,json
raw = sys.stdin.read()
try:
    print(json.loads(raw)["data"]["id"])
except Exception:
    sys.stderr.write("RESPONSE: " + raw[:400] + "\n")
    raise SystemExit(1)'; }
post() { curl -s -X POST "$API$1" -H "$J" -H "$AH" -d "$2"; }

echo "== 准备:商户 A/B + 各自商户账号(工单最小权限,bootstrap 装配) =="
ORG=$(post /api/v1/platform/business-entities "{\"idempotency_key\":\"mz2-org-$STAMP\",\"entity_name\":\"MZ2主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"mz2-$STAMP\",\"legal_representative\":\"MZ2\",\"bank_account_ref\":\"vault://complex/mz2/$STAMP\",\"status\":\"active\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
MA=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz2-ma-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ2商户A$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"A\",\"contact_phone\":\"18800000031\",\"status\":\"active\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
MB=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz2-mb-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ2商户B$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"B\",\"contact_phone\":\"18800000032\",\"status\":\"active\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
PW="Mz2-Ev-$STAMP-Aa1!"
UA=$(post /api/v1/access/users "{\"idempotency_key\":\"mz2-ua-$STAMP\",\"username\":\"mz2_ma_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"merchant_id\":\"$MA\",\"status\":\"active\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
UB=$(post /api/v1/access/users "{\"idempotency_key\":\"mz2-ub-$STAMP\",\"username\":\"mz2_mb_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"merchant_id\":\"$MB\",\"status\":\"active\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
ROLE=$(post /api/v1/access/roles "{\"idempotency_key\":\"mz2-role-$STAMP\",\"role_name\":\"MZ2商户工单$STAMP\",\"description\":\"商户专区工单最小权限\",\"permission_codes\":[\"merchant.service_work_order.view\",\"merchant.service_work_order.create\"],\"data_scope\":\"merchant\",\"operator_note\":\"mz2\",\"evidence_attachment_ids\":[\"att-mz2-$STAMP\"]}" | jid)
psql -q "$PSQL" -c "INSERT INTO user_roles (user_id, role_id) VALUES ('$UA', '$ROLE'), ('$UB', '$ROLE') ON CONFLICT DO NOTHING;"
echo "MA=$MA MB=$MB UA=$UA UB=$UB"

echo "== 1. 商户A账号提交工单(不传 merchant_id,期望强制落本商户) =="
UA_TOK=$(tok "mz2_ma_$STAMP" "$PW")
WO=$(curl -s -X POST "$API/api/v1/merchant-service/work-orders" -H "$J" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d "{\"idempotency_key\":\"mz2-wo-$STAMP\",\"work_order_type\":\"repair\",\"priority\":\"high\",\"title\":\"MZ2活证据报修$STAMP\",\"description\":\"商户专区自助提交\",\"reported_channel\":\"merchant_portal\"}")
echo "$WO" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['merchant_id']=='$MA', f\"expect merchant_id=$MA, got {d['merchant_id']}\"
print('PASS: work order created, merchant_id forced to own merchant', d['id'])
"

echo "== 2. 商户A只见本商户工单;商户B看不到A的工单 =="
curl -s "$API/api/v1/merchant-service/work-orders" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert rows, 'merchant A should see own work orders'
assert all(r['merchant_id']=='$MA' for r in rows), 'merchant A sees foreign work order!'
print(f'PASS: merchant A sees {len(rows)} work order(s), all own merchant')
"
UB_TOK=$(tok "mz2_mb_$STAMP" "$PW")
curl -s "$API/api/v1/merchant-service/work-orders" -H "Authorization: Bearer $UB_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert all(r['merchant_id']!='$MA' for r in rows), 'merchant B can see A work orders!'
print(f'PASS: merchant B sees {len(rows)} row(s), none from merchant A')
"

echo
echo "ALL LIVE EVIDENCE PASSED (merchant zone slice 2)"
