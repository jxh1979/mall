#!/usr/bin/env bash
# 商户专区切片5 活证据:费用通知接收人商户自助口径(PII 手机号,强制本商户)
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

echo "== 准备:商户 A/B + 商户A账号(仅 merchant.portal.fee_recipient.manage) =="
ORG=$(post /api/v1/platform/business-entities "{\"idempotency_key\":\"mz5-org-$STAMP\",\"entity_name\":\"MZ5主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"mz5-$STAMP\",\"legal_representative\":\"MZ5\",\"bank_account_ref\":\"vault://complex/mz5/$STAMP\",\"status\":\"active\",\"operator_note\":\"mz5\",\"evidence_attachment_ids\":[\"att-mz5-$STAMP\"]}" | jid)
MA=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz5-ma-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ5商户A$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"A\",\"contact_phone\":\"18800000051\",\"status\":\"active\",\"operator_note\":\"mz5\",\"evidence_attachment_ids\":[\"att-mz5-$STAMP\"]}" | jid)
MB=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz5-mb-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ5商户B$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"B\",\"contact_phone\":\"18800000052\",\"status\":\"active\",\"operator_note\":\"mz5\",\"evidence_attachment_ids\":[\"att-mz5-$STAMP\"]}" | jid)
PW="Mz5-Ev-$STAMP-Aa1!"
UA=$(post /api/v1/access/users "{\"idempotency_key\":\"mz5-ua-$STAMP\",\"username\":\"mz5_ma_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"merchant_id\":\"$MA\",\"status\":\"active\",\"operator_note\":\"mz5\",\"evidence_attachment_ids\":[\"att-mz5-$STAMP\"]}" | jid)
ROLE=$(post /api/v1/access/roles "{\"idempotency_key\":\"mz5-role-$STAMP\",\"role_name\":\"MZ5费用通知自助$STAMP\",\"description\":\"商户专区费用通知最小权限\",\"permission_codes\":[\"merchant.portal.fee_recipient.manage\"],\"data_scope\":\"merchant\",\"operator_note\":\"mz5\",\"evidence_attachment_ids\":[\"att-mz5-$STAMP\"]}" | jid)
psql -q "$PSQL" -c "INSERT INTO user_roles (user_id, role_id) VALUES ('$UA', '$ROLE') ON CONFLICT DO NOTHING;"
# 商户B已有一条接收人(平台口径创建),用于越权读探测
RB=$(post "/api/v1/merchant-billing/fee-notification-recipients" "{\"merchant_id\":\"$MB\",\"fee_event\":\"rent\",\"recipient_name\":\"B财务\",\"recipient_phone\":\"15500000052\",\"operator_user_id\":\"$UA\"}" | jid)
echo "MA=$MA MB=$MB UA=$UA RB=$RB"

UA_TOK=$(tok "mz5_ma_$STAMP" "$PW")
MH="Authorization: Bearer $UA_TOK"

echo "== 1. 商户A自助保存接收人:merchant_id/操作人被强制为登录主体(请求里伪造他商户) =="
RA=$(curl -s -X POST "$API/api/v1/merchant-billing/fee-notification-recipients" -H "$J" -H "$MH" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d "{\"merchant_id\":\"$MB\",\"fee_event\":\"property\",\"recipient_name\":\"A店长\",\"recipient_phone\":\"15500000051\",\"operator_user_id\":\"usr_forged\"}")
echo "$RA" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['merchant_id']=='$MA', f\"forged merchant accepted: {d['merchant_id']}\"
assert d['created_by']=='$UA', f\"forged operator accepted: {d['created_by']}\"
print('PASS: upsert forced to own merchant + actor operator', d['id'])
"
RA_ID=$(echo "$RA" | jid)

echo "== 2. 商户A list 只见本商户(请求参数指向他商户被忽略) =="
curl -s "$API/api/v1/merchant-billing/fee-notification-recipients?merchant_id=$MB" -H "$MH" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert rows, 'own recipients missing'
assert all(r['merchant_id']=='$MA' for r in rows), f'foreign recipients leaked'
print(f'PASS: list forced to own merchant, {len(rows)} row(s)')
"

echo "== 3. 商户A读/停用商户B接收人 → 403 CROSS_MERCHANT_FORBIDDEN =="
CODE=$(curl -s -o /tmp/mz5_get.json -w '%{http_code}' "$API/api/v1/merchant-billing/fee-notification-recipients/$RB" -H "$MH" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant")
[ "$CODE" = "403" ] && grep -q CROSS_MERCHANT_FORBIDDEN /tmp/mz5_get.json && echo "PASS: cross-merchant get rejected" || { head -c 200 /tmp/mz5_get.json; exit 1; }
CODE=$(curl -s -o /tmp/mz5_off.json -w '%{http_code}' -X POST "$API/api/v1/merchant-billing/fee-notification-recipients/$RB/deactivate" -H "$J" -H "$MH" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d '{}')
[ "$CODE" = "403" ] && grep -q CROSS_MERCHANT_FORBIDDEN /tmp/mz5_off.json && echo "PASS: cross-merchant deactivate rejected" || { head -c 200 /tmp/mz5_off.json; exit 1; }

echo "== 4. 商户A停用本商户接收人 → 生效 =="
curl -s -X POST "$API/api/v1/merchant-billing/fee-notification-recipients/$RA_ID/deactivate" -H "$J" -H "$MH" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant" -d '{}' | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['status']=='inactive', d
print('PASS: own recipient deactivated')
"

echo "== 5. 平台口径不变:admin 仍可跨商户管理 =="
curl -s "$API/api/v1/merchant-billing/fee-notification-recipients?merchant_id=$MB" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert any(r['id']=='$RB' for r in rows), 'platform scope broken'
print('PASS: platform admin still manages any merchant')
"

echo
echo "ALL LIVE EVIDENCE PASSED (merchant zone slice 5)"
