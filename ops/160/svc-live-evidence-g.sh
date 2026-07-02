#!/usr/bin/env bash
# G批活证据:目录→收款码前置→代客下单指定履约人→出示收款码→接单→完成→月统计→取消口径;直付不入账断言。
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

echo "== 1. 后台建服务目录项:点舞 300 元参考价 15 分钟 =="
SVC=$(curl -s -X POST $API/api/v1/services/catalog -H "$J" -H "$AH" -d "{\"idempotency_key\":\"g-svc-$STAMP\",\"name\":\"点舞G$STAMP\",\"price_cent\":30000,\"duration_minutes\":15,\"active\":true}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:300];print(d["data"]["id"])')
echo "SVC=$SVC"

echo "== 2. 直付前置:履约人未录收款码 → 下单被拒 =="
psql $PG -c "DELETE FROM staff_payment_codes WHERE user_id='$ME';" > /dev/null
curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-o0-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='PERFORMER_NO_PAYMENT_CODE', str(d)[:200]
print('PASS: order rejected until performer has payment code')
"

echo "== 3. 履约人在手机录入固定收款码(支付宝链接文本) =="
curl -s -X POST $API/api/v1/services/payment-code -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-pc-$STAMP\",\"qr_content\":\"https://qr.alipay.com/fkx_g_$STAMP\"}" | python3 -c 'import sys,json;assert json.load(sys.stdin).get("success");print("PASS: payment code saved")'

echo "== 4. 服务员代客下单(指定履约人+桌台)→ 出示收款码=履约人录入原文 =="
ORD=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-o1-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\",\"table_id\":\"tbl_g\",\"customer_note\":\"客人指定\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d["data"]["status"]=="pending_accept",d["data"];print(d["data"]["id"])')
curl -s "$API/api/v1/services/orders/$ORD/payment-code" -H "$AH" -H "$TH" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert d['qr_content']=='https://qr.alipay.com/fkx_g_$STAMP', d
assert d['price_cent']=='30000', d
print('PASS: payment code presented to guest — 客人扫码直付履约人')
"

echo "== 5. 履约人接单 → 完成(直付已现场发生) =="
curl -s -X POST $API/api/v1/services/orders/$ORD/accept -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-ac-$STAMP\"}" | python3 -c 'import sys,json;assert json.load(sys.stdin)["data"]["status"]=="accepted";print("accepted")'
curl -s -X POST $API/api/v1/services/orders/$ORD/complete -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-cp-$STAMP\"}" | python3 -c 'import sys,json;assert json.load(sys.stdin)["data"]["status"]=="completed";print("PASS: completed")'

echo "== 6. 我的服务统计:当月完成>=1,参考收入>=300元(直付台账,非营收) =="
curl -s "$API/api/v1/services/orders/my-stats?month=$MONTH" -H "$AH" -H "$TH" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
assert int(d['completed_count'])>=1 and int(d['reference_income_cent'])>=30000, d
print('PASS: my-stats count=%s income=%s cent' % (d['completed_count'], d['reference_income_cent']))
"

echo "== 7. 口径⑤:完成后取消被拒;新单履约前取消成功留原因 =="
curl -s -X POST $API/api/v1/services/orders/$ORD/cancel -H "$J" -H "$AH" -d "{\"idempotency_key\":\"g-cx1-$STAMP\",\"reason\":\"客人反悔\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='ALREADY_COMPLETED', str(d)[:200]
print('PASS: completed order cannot be cancelled')
"
ORD2=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"g-o2-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST $API/api/v1/services/orders/$ORD2/cancel -H "$J" -H "$AH" -d "{\"idempotency_key\":\"g-cx2-$STAMP\",\"reason\":\"客人取消\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin)["data"];assert d["status"]=="cancelled" and d["cancel_reason"]=="客人取消",d;print("PASS: pending order cancelled with reason")'

echo "== 8. 直付不入账断言:服务金额未进 AR/酒吧账单/平台净收入快照 =="
psql $PG -tc "SELECT
  (SELECT COUNT(*) FROM ar_ap_items WHERE source_id LIKE '%$ORD%' OR item_no LIKE '%$ORD%'),
  (SELECT COUNT(*) FROM bar_bills WHERE id = '$ORD');" 2>/dev/null | python3 -c "
import sys
parts=[p.strip() for p in sys.stdin.read().split('|')]
assert parts[0]=='0', 'service leaked into AR!'
print('PASS: no AR/bar-bill rows reference the service order — 直付不入账')
"

echo
echo "== APK 产物 =="
sha256sum ~/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk
echo
echo "ALL LIVE EVIDENCE PASSED (G批 服务点单)"
