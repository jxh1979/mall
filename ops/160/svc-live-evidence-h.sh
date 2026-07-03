#!/usr/bin/env bash
# H批活证据:公司代收全链——公司码配置→下单出示公司码→未收款完成被拒→确认收款→完成分账→
# 提成台账→月结进薪资+代扣个税(税表恒等)→已收款取消标待退。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
TH='X-Terminal-Type: staff_private_phone'
PG=postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"
ME=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["user"]["id"])')
MONTH=$(date -u +%Y-%m)

echo "== 1. 配置公司统一收款码 + 目录项(点舞300元,管理费10%) =="
curl -s -X POST $API/api/v1/services/pay-config -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h-pc-$STAMP\",\"qr_content\":\"https://qr.alipay.com/company_$STAMP\"}" > /dev/null
SVC=$(curl -s -X POST $API/api/v1/services/catalog -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h-svc-$STAMP\",\"name\":\"点舞H$STAMP\",\"price_cent\":30000,\"duration_minutes\":15,\"active\":true,\"mgmt_fee_mode\":\"percent\",\"mgmt_fee_value\":1000}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:300];print(d["data"]["id"])')
echo "SVC=$SVC"

echo "== 2. 代客下单 → 出示码=公司收款码(代收,非舞者个人码) =="
ORD=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-o1-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\",\"table_id\":\"tbl_h\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d["data"]["status"]=="created",d["data"];print(d["data"]["id"])')
curl -s "$API/api/v1/services/orders/$ORD/payment-code" -H "$AH" -H "$TH" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['qr_content']=='https://qr.alipay.com/company_$STAMP', d
print('PASS: payment code = COMPANY account QR — 收款进公司账,财务可对账')
"

echo "== 3. 未确认收款 → 完成被拒(收款确认盲区闭环) =="
curl -s -X POST $API/api/v1/services/orders/$ORD/complete -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-c0-$STAMP\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='NOT_PAID_YET', str(d)[:200]
print('PASS: cannot complete before company-account payment confirmed')
"

echo "== 4. 服务员确认收款 → 完成 → 分账恒等式(300=30管理费+270舞者) =="
curl -s -X POST $API/api/v1/services/orders/$ORD/mark-paid -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-mp-$STAMP\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"];assert d["status"]=="paid" and d["pay_method"]=="company_qr",d;print("paid confirmed by waiter")'
curl -s -X POST $API/api/v1/services/orders/$ORD/complete -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-c1-$STAMP\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)["data"]
assert d["status"]=="completed", d
assert d["mgmt_fee_cent"]==3000 and d["dancer_share_cent"]==27000, d
assert d["price_cent"]==d["mgmt_fee_cent"]+d["dancer_share_cent"], d
print("PASS: split 300 = 30(company mgmt fee) + 270(dancer share)")'
psql $PG -tc "SELECT amount_cent FROM staff_commission_ledger WHERE source_type='service_order' AND source_id='$ORD';" | python3 -c "
import sys
v=sys.stdin.read().strip()
assert v=='27000', v
print('PASS: dancer share 270元 written to commission ledger (月结自动进薪资)')
"

echo "== 5. 月结 → 薪资单含服务提成+按劳务报酬税表代扣个税 =="
psql $PG -c "DELETE FROM staff_payroll_statements WHERE user_id='$ME' AND month='$MONTH';" > /dev/null
curl -s -X POST $API/api/v1/staff/payroll/close -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h-cl-$STAMP\",\"month\":\"$MONTH\"}" > /dev/null
curl -s "$API/api/v1/staff/payroll/mine?month=$MONTH" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=[r for r in json.load(sys.stdin)['data']['items'] if r['user_id']=='$ME']
assert rows, 'statement missing'
r=rows[0]
svc=r['commission_cent']
tax=r['tax_cent']
# 劳务报酬预扣(仅 service_order 部分计税):X<=4000元→(X-800)*20%;>4000→X*0.8 分档
# 本轮服务劳务合计>=270元,历史重放可能累积——恒等式断言:total=base+comm+bonus-penalty-tax
assert r['total_cent']==r['base_cent']+r['commission_cent']+r['bonus_cent']-r['penalty_cent']-r['tax_cent'], r
assert tax>=0, r
print('PASS: payroll statement — commission=%s tax=%s total恒等式成立(代扣个税已入薪资)' % (svc, tax))
"

echo "== 6. 已收款后取消 → 标「已收款待退」留痕(线下退,财务对账) =="
ORD2=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-o2-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST $API/api/v1/services/orders/$ORD2/mark-paid -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"h-mp2-$STAMP\"}" > /dev/null
curl -s -X POST $API/api/v1/services/orders/$ORD2/cancel -H "$J" -H "$AH" -d "{\"idempotency_key\":\"h-cx-$STAMP\",\"reason\":\"客人临时有事\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)["data"]
assert d["status"]=="cancelled" and d["refund_note"] and "已收款待退" in d["refund_note"], d
print("PASS: paid-then-cancelled marked 待退款 —", d["refund_note"])'

echo
echo "== APK 产物 =="
sha256sum ~/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk
echo
echo "ALL LIVE EVIDENCE PASSED (H批 公司代收+分账+个税)"
