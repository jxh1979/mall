#!/usr/bin/env bash
# 商户专区切片1 活证据:商户账号只见本商户账单 + merchant_portal 终端总开关后端硬拒
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
fail() { echo "EVIDENCE-FAIL: $1"; exit 1; }

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }

# 管理账号口径按实例探测:user-systemd 实例=admin/admin-pass;手工 dev 实例=~/.complex/complex.env 的 P0 账号
ADMIN_TOK=$(tok admin admin-pass 2>/dev/null || true)
if [ -z "${ADMIN_TOK:-}" ]; then
  P0U=$(grep '^P0_ADMIN_USERNAME=' ~/.complex/complex.env | cut -d= -f2)
  P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
  ADMIN_TOK=$(tok "$P0U" "$P0P")
fi
AH="Authorization: Bearer $ADMIN_TOK"

post() { # path json -> data
  curl -s -X POST "$API$1" -H "$J" -H "$AH" -d "$2"
}
jid() { python3 -c 'import sys,json
raw = sys.stdin.read()
try:
    print(json.loads(raw)["data"]["id"])
except Exception:
    sys.stderr.write("RESPONSE: " + raw[:400] + "\n")
    raise SystemExit(1)'; }

echo "== 1. 建经营主体 + 商户 M1/M2 + 各一张账单 =="
ORG=$(post /api/v1/platform/business-entities "{\"idempotency_key\":\"mz-ev-org-$STAMP\",\"entity_name\":\"MZ活证据主体$STAMP\",\"entity_role\":\"operation\",\"unified_social_credit_code\":\"mz-ev-$STAMP\",\"legal_representative\":\"MZ\",\"bank_account_ref\":\"vault://complex/mz/$STAMP\",\"status\":\"active\",\"operator_note\":\"merchant zone live evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
M1=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz-ev-m1-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ商户甲$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"甲\",\"contact_phone\":\"18800000021\",\"status\":\"active\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
M2=$(post /api/v1/platform/merchants "{\"idempotency_key\":\"mz-ev-m2-$STAMP\",\"business_entity_id\":\"$ORG\",\"merchant_name\":\"MZ商户乙$STAMP\",\"merchant_type\":\"other\",\"contact_name\":\"乙\",\"contact_phone\":\"18800000022\",\"status\":\"active\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
echo "ORG=$ORG M1=$M1 M2=$M2"

AR1=$(post /api/v1/finance/ar-ap-items "{\"idempotency_key\":\"mz-ev-ar1-$STAMP\",\"direction\":\"receivable\",\"counterparty_id\":\"$M1\",\"source_type\":\"manual\",\"source_id\":\"mz-ev-src1-$STAMP\",\"amount_cent\":123400,\"due_date\":\"2026-07-15\",\"accounting_period\":\"2026-06\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
AR2=$(post /api/v1/finance/ar-ap-items "{\"idempotency_key\":\"mz-ev-ar2-$STAMP\",\"direction\":\"receivable\",\"counterparty_id\":\"$M2\",\"source_type\":\"manual\",\"source_id\":\"mz-ev-src2-$STAMP\",\"amount_cent\":567800,\"due_date\":\"2026-07-15\",\"accounting_period\":\"2026-06\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
B1=$(post /api/v1/merchant-billing/bills "{\"idempotency_key\":\"mz-ev-b1-$STAMP\",\"merchant_id\":\"$M1\",\"ar_ap_item_id\":\"$AR1\",\"bill_period_start\":\"2026-06-01\",\"bill_period_end\":\"2026-06-30\",\"due_date\":\"2026-07-15\",\"total_amount_cent\":123400,\"source_type\":\"manual\",\"lines\":[{\"fee_type\":\"rent\",\"description\":\"MZ甲租金\",\"amount_cent\":123400}]}" | jid)
B2=$(post /api/v1/merchant-billing/bills "{\"idempotency_key\":\"mz-ev-b2-$STAMP\",\"merchant_id\":\"$M2\",\"ar_ap_item_id\":\"$AR2\",\"bill_period_start\":\"2026-06-01\",\"bill_period_end\":\"2026-06-30\",\"due_date\":\"2026-07-15\",\"total_amount_cent\":567800,\"source_type\":\"manual\",\"lines\":[{\"fee_type\":\"property_fee\",\"description\":\"MZ乙物业费\",\"amount_cent\":567800}]}" | jid)
echo "B1=$B1(M1) B2=$B2(M2)"

echo "== 2. 建商户账号(merchant_id=M1)+ 最小角色 merchant.billing.view =="
PW="Mz-Ev-$STAMP-Aa1!"
U1=$(post /api/v1/access/users "{\"idempotency_key\":\"mz-ev-u1-$STAMP\",\"username\":\"mz_ev_merchant_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"merchant_id\":\"$M1\",\"status\":\"active\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
ROLE=$(post /api/v1/access/roles "{\"idempotency_key\":\"mz-ev-role-$STAMP\",\"role_name\":\"MZ商户账单只读$STAMP\",\"description\":\"商户专区活证据最小权限\",\"permission_codes\":[\"merchant.billing.view\"],\"data_scope\":\"merchant\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
# 授权装配:merchant.billing.view 为高危权限,API 直配走 V169 审批链(有独立职级/rank 闸,另有专项验收)。
# 本活证据聚焦商户身份闸+终端开关,装配按"初始化种子"口径直接落 user_roles(与生产 bootstrap 同范式)。
psql -q postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex -c "INSERT INTO user_roles (user_id, role_id) VALUES ('$U1', '$ROLE') ON CONFLICT DO NOTHING;"
echo "U1=$U1 ROLE=$ROLE (role granted via bootstrap SQL)"

echo "== 3. 商户账号登录,GET bills(X-Terminal-Type: merchant_portal)只见本商户 =="
U1_TOK=$(tok "mz_ev_merchant_$STAMP" "$PW")
BILLS=$(curl -s "$API/api/v1/merchant-billing/bills" -H "Authorization: Bearer $U1_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant")
echo "$BILLS" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']['items']
mids=set(b['merchant_id'] for b in d)
assert mids=={'$M1'}, f'expect only M1, got {mids}'
assert any(b['id']=='$B1' for b in d), 'B1 missing'
assert all(b['id']!='$B2' for b in d), 'B2 (other merchant) leaked!'
print(f'PASS: merchant user sees {len(d)} bill(s), all merchant_id=$M1; cross-merchant bill invisible')
"

echo "== 4. 关闭 merchant_portal 总开关 → 商户请求被后端硬拒 403 TERMINAL_DISABLED =="
curl -s -X PUT "$API/api/v1/terminals/capability-switches/tcs_term_merchant_portal" -H "$J" -H "$AH" -d "{\"idempotency_key\":\"mz-ev-off-$STAMP\",\"enabled\":false,\"reason\":\"MZ活证据演练关闭\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"];print("switch:",d["id"],"enabled=",d["enabled"])'
CODE=$(curl -s -o /tmp/mz_ev_denied.json -w '%{http_code}' "$API/api/v1/merchant-billing/bills" -H "Authorization: Bearer $U1_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant")
cat /tmp/mz_ev_denied.json | head -c 300; echo
[ "$CODE" = "403" ] || fail "expect 403 when merchant_portal disabled, got $CODE"
grep -q TERMINAL_DISABLED /tmp/mz_ev_denied.json || fail "expect TERMINAL_DISABLED body"
echo "PASS: merchant_portal master switch off => 403 TERMINAL_DISABLED"

echo "== 5. 商户端 effective 关闭行可拉取(前端菜单 gating 数据源,自救豁免路径) =="
curl -s "$API/api/v1/terminals/capability-switches/effective" -H "Authorization: Bearer $U1_TOK" -H "X-Terminal-Type: merchant_portal" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert any(r['terminal_type']=='merchant_portal' and r['scope_type']=='global' for r in rows), f'expect global disabled row, got {rows}'
print('PASS: effective returns merchant_portal global disabled row ->前端除自救组外全隐')
"

echo "== 6. 恢复开关 → 商户请求恢复 200 =="
curl -s -X PUT "$API/api/v1/terminals/capability-switches/tcs_term_merchant_portal" -H "$J" -H "$AH" -d "{\"idempotency_key\":\"mz-ev-on-$STAMP\",\"enabled\":true,\"reason\":\"MZ活证据演练恢复\"}" > /dev/null
CODE2=$(curl -s -o /dev/null -w '%{http_code}' "$API/api/v1/merchant-billing/bills" -H "Authorization: Bearer $U1_TOK" -H "X-Terminal-Type: merchant_portal" -H "X-Module-Code: merchant")
[ "$CODE2" = "200" ] || fail "expect 200 after re-enable, got $CODE2"
echo "PASS: switch restored, merchant requests back to 200"

echo "== 7. 无商户归属的普通员工按权限仍拿不到数据口径(菜单不显示;此处验证后端 fail-closed) =="
U2=$(post /api/v1/access/users "{\"idempotency_key\":\"mz-ev-u2-$STAMP\",\"username\":\"mz_ev_staff_$STAMP\",\"password\":\"$PW\",\"account_type\":\"staff\",\"organization_id\":\"$ORG\",\"status\":\"active\",\"operator_note\":\"mz evidence\",\"evidence_attachment_ids\":[\"att-mz-$STAMP\"]}" | jid)
psql -q postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex -c "INSERT INTO user_roles (user_id, role_id) VALUES ('$U2', '$ROLE') ON CONFLICT DO NOTHING;"
U2_TOK=$(tok "mz_ev_staff_$STAMP" "$PW")
CODE3=$(curl -s -o /tmp/mz_ev_staff.json -w '%{http_code}' "$API/api/v1/merchant-billing/bills" -H "Authorization: Bearer $U2_TOK" -H "X-Terminal-Type: web")
head -c 200 /tmp/mz_ev_staff.json; echo
[ "$CODE3" = "403" ] || fail "expect 403 MERCHANT_SCOPE_REQUIRED for staff without merchant, got $CODE3"
grep -q MERCHANT_SCOPE_REQUIRED /tmp/mz_ev_staff.json || fail "expect MERCHANT_SCOPE_REQUIRED body"
echo "PASS: staff without merchant scope => 403 MERCHANT_SCOPE_REQUIRED (fail-closed)"

echo
echo "ALL LIVE EVIDENCE PASSED (merchant zone slice 1)"
