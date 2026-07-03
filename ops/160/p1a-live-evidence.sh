#!/usr/bin/env bash
# 审计P1-A活证据:服务点单现场资金动作终端 fail-closed——
# 缺省/空白 X-Terminal-Type 一律 403 TERMINAL_NOT_ALLOWED(私人手机绕过被堵);
# 带公司工作手机终端头则放行到业务校验。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
WORK='X-Terminal-Type: staff_work_phone'

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"

echo "== 1. 目录项(下单前置) =="
SVC=$(curl -s -X POST $API/api/v1/services/catalog -H "$J" -H "$AH" -H "$WORK" -d "{\"idempotency_key\":\"p1a-svc-$STAMP\",\"name\":\"点舞P1A$STAMP\",\"price_cent\":30000,\"duration_minutes\":15,\"active\":true,\"mgmt_fee_mode\":\"percent\",\"mgmt_fee_value\":1000}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:200];print(d["data"]["id"])')
echo "SVC=$SVC"

echo "== 2. 下单缺终端头 → 403 TERMINAL_NOT_ALLOWED(fail-closed,私人手机/无标识绕过被堵) =="
curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1a-o-noh-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"usr_x\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='TERMINAL_NOT_ALLOWED', str(d)[:200]
print('PASS: 下单缺 X-Terminal-Type → 403 TERMINAL_NOT_ALLOWED')
"

echo "== 3. 下单带私人手机终端头 → 仍 403(现场资金动作禁私人手机) =="
curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H 'X-Terminal-Type: staff_private_phone' -d "{\"idempotency_key\":\"p1a-o-pp-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"usr_x\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='TERMINAL_NOT_ALLOWED', str(d)[:200]
print('PASS: 私人手机下单 → 403 TERMINAL_NOT_ALLOWED')
"

echo "== 4. 下单带公司工作手机终端头 → 放行到业务(不再被终端闸挡) =="
curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$WORK" -d "{\"idempotency_key\":\"p1a-o-work-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"usr_x\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
# admin=平台管理员空 scope,跨项目守卫放行;下单成功=终端闸已放行公司设备。
assert d.get('success') is True and d['data']['status']=='created', str(d)[:200]
print('PASS: 公司工作手机下单放行 status=created(终端闸对公司设备放行)')
"

echo "== 5. mark-paid 缺终端头 → 403(资金确认动作同 fail-closed) =="
ORD=$(curl -s -X POST $API/api/v1/services/orders -H "$J" -H "$AH" -H "$WORK" -d "{\"idempotency_key\":\"p1a-o2-$STAMP\",\"catalog_item_id\":\"$SVC\",\"performer_user_id\":\"usr_x\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST $API/api/v1/services/orders/$ORD/mark-paid -H "$J" -H "$AH" -d "{\"idempotency_key\":\"p1a-mp-noh-$STAMP\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False and d.get('detail_code')=='TERMINAL_NOT_ALLOWED', str(d)[:200]
print('PASS: mark-paid 缺终端头 → 403 TERMINAL_NOT_ALLOWED')
"

echo
echo "ALL LIVE EVIDENCE PASSED (审计P1-A 终端 fail-closed;跨项目守卫由 StaffScopeTests/ServiceOrderScopeTests 单测钉牢)"
