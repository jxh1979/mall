#!/usr/bin/env bash
# 三个 P1 修复活证据(真实后端 127.0.0.1:8080 + 部署库):
#  P1-1 储值 consume 卡范围越权:2-token 跨身份流,逻辑由 MemberWalletServiceTests 绿测钉死
#        (mock 商户 scope,消费他商户/通用卡→CROSS_MERCHANT_FORBIDDEN,与 refund/freeze 同一守卫);此处不重造双 token。
#  P1-2 服务点单提成幂等:真实下单→收款→完成→台账恰一条;再直插同单提成→部分唯一索引拒(部署库已生效)。
#  P1-3 能源费单源:合同域按面积/按用量填固定能源费金额→下单即拒;by_contract 包干填→放行(校验按计费方式区分)。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
TH='X-Terminal-Type: web'
PG=postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"
ME=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["user"]["id"])')

echo "== P1-2 (1/3): 配置公司码 + 目录项(点舞300元,管理费10%) =="
curl -s -X POST $API/api/v1/services/pay-config -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1-pc-$STAMP\",\"qr_content\":\"https://qr.alipay.com/company_$STAMP\"}" > /dev/null
SVC=$(curl -s -X POST $API/api/v1/services/catalog -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1-svc-$STAMP\",\"name\":\"点舞P1$STAMP\",\"price_cent\":30000,\"duration_minutes\":15,\"active\":true,\"mgmt_fee_mode\":\"percent\",\"mgmt_fee_value\":1000}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:300];print(d["data"]["id"])')

echo "== P1-2 (2/3): 下单→确认收款→完成→台账恰一条提成(270元) =="
ORD=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"p1-o-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"$ME\",\"table_id\":\"tbl_p1\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST $API/api/v1/services/orders/$ORD/mark-paid -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"p1-mp-$STAMP\"}" > /dev/null
curl -s -X POST $API/api/v1/services/orders/$ORD/complete -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"p1-c-$STAMP\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)["data"]
assert d["status"]=="completed" and d["dancer_share_cent"]==27000, d
print("completed, dancer_share=270")'
psql $PG -tc "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id='$ORD';" | python3 -c "
import sys; v=sys.stdin.read().strip(); assert v=='1', v; print('PASS: exactly 1 commission row for the order')"

echo "== P1-2 (3/3): 直插同单第二条提成 → 部分唯一索引拒(部署库已建索引) =="
if psql $PG -c "INSERT INTO staff_commission_ledger (id,user_id,month,source_type,source_id,amount_cent,created_at) VALUES ('cml_dup_$STAMP','$ME','2099-01','service_order','$ORD',27000,now());" 2>/tmp/p1dup.err; then
  echo "FAIL: duplicate service_order commission was accepted (index missing!)"; exit 1
else
  grep -qi 'uq_commission_service_order\|duplicate key' /tmp/p1dup.err && echo "PASS: duplicate rejected by uq_commission_service_order" || { echo "FAIL: rejected but not by our index:"; cat /tmp/p1dup.err; exit 1; }
fi

echo "== P1-3 (1/2): 按用量合同填固定能源费金额 → 下单即拒 =="
curl -s -X POST $API/api/v1/contracts -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1-ct-bad-$STAMP\",\"energy_fee_basis\":\"by_usage\",\"rent_terms\":{\"energyFeeCent\":5000}}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False, str(d)[:200]
assert 'energyFeeCent' in (d.get('message') or ''), d
print('PASS: metered contract rejects fixed energyFeeCent —', d.get('message'))"

echo "== P1-3 (2/2): by_contract 包干填固定能源费 → 越过能源校验(卡在后续必填项,证明按计费方式区分) =="
curl -s -X POST $API/api/v1/contracts -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1-ct-ok-$STAMP\",\"energy_fee_basis\":\"by_contract\",\"rent_terms\":{\"energyFeeCent\":5000}}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
# 应越过能源校验:要么成功,要么因后续必填(merchant_id 等)报错但 message 不含 energyFeeCent。
msg=d.get('message') or ''
assert 'energyFeeCent' not in msg, ('by_contract 不应被能源校验拦:', d)
print('PASS: by_contract fixed energy fee passes energy validation (blocked later on other required fields, msg=%r)' % msg[:60])"

echo
echo "ALL LIVE EVIDENCE PASSED (P1-2 提成幂等 / P1-3 能源费单源校验;P1-1 见 MemberWalletServiceTests 绿测)"
