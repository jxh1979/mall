#!/usr/bin/env bash
# 商户专区切片3/4 活证据:我的合同/我的对账只读隔离 + 结算异议留言(跨商户拒/重复拒)
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
PSQL="postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex"

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }
P0U=$(grep '^P0_ADMIN_USERNAME=' ~/.complex/complex.env | cut -d= -f2)
P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
ADMIN_TOK=$(tok "${P0U:-admin}" "${P0P:-admin-pass}")
AH="Authorization: Bearer $ADMIN_TOK"
jid() { python3 -c 'import sys,json
raw = sys.stdin.read()
try:
    print(json.loads(raw)["data"]["id"])
except Exception:
    sys.stderr.write("RESPONSE: " + raw[:400] + "\n")
    raise SystemExit(1)'; }
post() { curl -s -X POST "$API$1" -H "$J" -H "$AH" -d "$2"; }

echo "== 准备:商户 A/B、商户A账号(合同+对账最小权限)、SQL bootstrap 合同与结算单 =="
ORG=$(post /api/v1/platform/business-entities "{\"idempotency_key\":\"mz3-org-$STAMP\",\"entity_name\":\"MZ3主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"mz3-$STAMP\",\"legal_representative\":\"MZ3\",\"bank_account_ref\":\"vault://complex/mz3/$STAMP\",\"status\":\"active\",\"operator_note\":\"mz3\",\"evidence_attachment_ids\":[\"att-mz3-$STAMP\"]}" | jid)
MA=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz3-ma-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ3商户A$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"A\",\"contact_phone\":\"18800000041\",\"status\":\"active\",\"operator_note\":\"mz3\",\"evidence_attachment_ids\":[\"att-mz3-$STAMP\"]}" | jid)
MB=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz3-mb-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ3商户B$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"B\",\"contact_phone\":\"18800000042\",\"status\":\"active\",\"operator_note\":\"mz3\",\"evidence_attachment_ids\":[\"att-mz3-$STAMP\"]}" | jid)
PW="Mz3-Ev-$STAMP-Aa1!"
UA=$(post /api/v1/access/users "{\"idempotency_key\":\"mz3-ua-$STAMP\",\"username\":\"mz3_ma_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"merchant_id\":\"$MA\",\"status\":\"active\",\"operator_note\":\"mz3\",\"evidence_attachment_ids\":[\"att-mz3-$STAMP\"]}" | jid)
ROLE=$(post /api/v1/access/roles "{\"idempotency_key\":\"mz3-role-$STAMP\",\"role_name\":\"MZ3商户合同对账$STAMP\",\"description\":\"商户专区合同/对账最小权限\",\"permission_codes\":[\"contract.view\",\"merchant.portal.settlement.view\",\"merchant.portal.settlement.dispute\"],\"data_scope\":\"merchant\",\"operator_note\":\"mz3\",\"evidence_attachment_ids\":[\"att-mz3-$STAMP\"]}" | jid)
psql -q "$PSQL" -c "INSERT INTO user_roles (user_id, role_id) VALUES ('$UA', '$ROLE') ON CONFLICT DO NOTHING;"
# 合同/结算数据 bootstrap(被测物=商户自助读隔离与异议动作,非合同审批流)
psql -q "$PSQL" <<SQL
INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date, lifecycle_status, created_at, updated_at)
VALUES ('ct_mz3_a_$STAMP', '$MA', '$ORG', '$ORG', '2026-01-01', '2026-12-31', 'effective', now(), now()),
       ('ct_mz3_b_$STAMP', '$MB', '$ORG', '$ORG', '2026-01-01', '2026-12-31', 'effective', now(), now());
INSERT INTO contract_settlements (id, contract_id, trigger_type, status, created_at, updated_at)
VALUES ('cst_mz3_a_$STAMP', 'ct_mz3_a_$STAMP', 'move_out', 'pending_settlement', now(), now()),
       ('cst_mz3_b_$STAMP', 'ct_mz3_b_$STAMP', 'move_out', 'pending_settlement', now(), now());
SQL
echo "MA=$MA MB=$MB UA=$UA"

UA_TOK=$(tok "mz3_ma_$STAMP" "$PW")
MH() { echo "-H"; }

echo "== 1. 我的合同:只见本商户 =="
curl -s "$API/api/v1/merchant-portal/contracts" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
ids=[r['id'] for r in rows]
assert 'ct_mz3_a_$STAMP' in ids, f'own contract missing: {ids}'
assert all(r['merchant_id']=='$MA' for r in rows), f'foreign contract leaked: {ids}'
print(f'PASS: merchant A sees {len(rows)} contract(s), all own; fields:', sorted(rows[0].keys()))
"

echo "== 2. 我的对账:只见本商户结算单 =="
curl -s "$API/api/v1/merchant-portal/settlements" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
ids=[r['id'] for r in rows]
assert 'cst_mz3_a_$STAMP' in ids, f'own settlement missing: {ids}'
assert 'cst_mz3_b_$STAMP' not in ids, 'foreign settlement leaked!'
print(f'PASS: merchant A sees {len(rows)} settlement(s), foreign invisible')
"

echo "== 3. 异议留言:本商户结算单挂起为有争议 =="
curl -s -X POST "$API/api/v1/merchant-portal/settlements/cst_mz3_a_$STAMP/dispute" -H "$J" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d "{\"idempotency_key\":\"mz3-disp-$STAMP\",\"reason\":\"结算金额与合同约定不符,请复核\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['status']=='disputed', d
assert '不符' in d['dispute_reason']
print('PASS: settlement disputed with merchant reason')
"

echo "== 4. 跨商户异议被拒 CROSS_MERCHANT_FORBIDDEN =="
CODE=$(curl -s -o /tmp/mz3_cross.json -w '%{http_code}' -X POST "$API/api/v1/merchant-portal/settlements/cst_mz3_b_$STAMP/dispute" -H "$J" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d "{\"idempotency_key\":\"mz3-cross-$STAMP\",\"reason\":\"越权尝试\"}")
[ "$CODE" = "403" ] && grep -q CROSS_MERCHANT_FORBIDDEN /tmp/mz3_cross.json && echo "PASS: cross-merchant dispute rejected 403" || { head -c 200 /tmp/mz3_cross.json; echo "FAIL cross($CODE)"; exit 1; }

echo "== 5. 重复异议被拒 SETTLEMENT_NOT_DISPUTABLE =="
CODE=$(curl -s -o /tmp/mz3_dup.json -w '%{http_code}' -X POST "$API/api/v1/merchant-portal/settlements/cst_mz3_a_$STAMP/dispute" -H "$J" -H "Authorization: Bearer $UA_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d "{\"idempotency_key\":\"mz3-dup-$STAMP\",\"reason\":\"重复\"}")
[ "$CODE" = "409" ] && grep -q SETTLEMENT_NOT_DISPUTABLE /tmp/mz3_dup.json && echo "PASS: duplicate dispute rejected 409" || { head -c 200 /tmp/mz3_dup.json; echo "FAIL dup($CODE)"; exit 1; }

echo "== 6. 平台裁决(既有 resolve-dispute)继续闭环 =="
curl -s -X POST "$API/api/v1/contracts/settlements/cst_mz3_a_$STAMP/resolve-dispute" -H "$J" -H "$AH" -d "{\"idempotency_key\":\"mz3-resolve-$STAMP\",\"operator_note\":\"活证据裁决\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['status']=='pending_settlement', d
print('PASS: platform resolve-dispute returns settlement to pending_settlement')
"

echo
echo "ALL LIVE EVIDENCE PASSED (merchant zone slice 3/4)"
