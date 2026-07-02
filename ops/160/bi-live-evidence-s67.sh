#!/usr/bin/env bash
# BI S6 活证据:真实后端——联合通票(酒吧+游乐)分项单价漏填→发布400拦截;补齐→发布成功。
# S7(member.new 排除注销/黑名单)由真库集成测试 countNewMembersExcludesDeregisteredAndBlacklisted 覆盖。
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

echo "== 1. 造联合通票:酒吧权益(单价99元)+游乐权益(漏填单价) =="
TT=$(curl -s -X POST $API/api/v1/ticketing/ticket-types -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-tt-$STAMP\",\"ticket_name\":\"S6联合通票$STAMP\",\"base_calendar_price_mode\":\"fixed\",\"weekday_price_cent\":12900,\"weekend_price_cent\":12900,\"holiday_price_cent\":12900,\"stock_required\":false}" | jid)
TPL_BAR=$(curl -s -X POST $API/api/v1/ticketing/entitlement-templates -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-tpl-bar-$STAMP\",\"name\":\"S6酒吧饮品$STAMP\",\"entitlement_type\":\"bar_drink\",\"default_total_times\":1,\"default_value_cent\":9900}" | jid)
TPL_RIDE=$(curl -s -X POST $API/api/v1/ticketing/entitlement-templates -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-tpl-ride-$STAMP\",\"name\":\"S6游乐项目$STAMP\",\"entitlement_type\":\"amusement_ride\",\"default_total_times\":1,\"default_value_cent\":3000}" | jid)
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT/entitlements -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-bind-bar-$STAMP\",\"entitlement_template_id\":\"$TPL_BAR\",\"quantity\":1,\"component_price_cent\":9900}" > /dev/null
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT/entitlements -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-bind-ride-$STAMP\",\"entitlement_template_id\":\"$TPL_RIDE\",\"quantity\":1}" > /dev/null
echo "TT=$TT (bar 有单价 / ride 漏填)"

echo "== 2. 发布 → 必须 400 拦截(不许均摊兜底) =="
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT/publish -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-pub-1-$STAMP\",\"action\":\"publish\",\"reason\":\"s6\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False, d
assert d.get('detail_code')=='VALIDATION_FAILED', d
assert 'component_price_cent' in d.get('message',''), d
print('PASS: publish blocked 400 —', d['message'][:90])
"

echo "== 3. 干净通票(两分项单价填全 99:30)→ 发布成功 =="
TT2=$(curl -s -X POST $API/api/v1/ticketing/ticket-types -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-tt2-$STAMP\",\"ticket_name\":\"S6合规通票$STAMP\",\"base_calendar_price_mode\":\"fixed\",\"weekday_price_cent\":12900,\"weekend_price_cent\":12900,\"holiday_price_cent\":12900,\"stock_required\":false}" | jid)
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT2/entitlements -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-bind2-bar-$STAMP\",\"entitlement_template_id\":\"$TPL_BAR\",\"quantity\":1,\"component_price_cent\":9900}" > /dev/null
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT2/entitlements -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-bind2-ride-$STAMP\",\"entitlement_template_id\":\"$TPL_RIDE\",\"quantity\":1,\"component_price_cent\":3000}" > /dev/null
curl -s -X POST $API/api/v1/ticketing/ticket-types/$TT2/publish -H "$J" -H "$AH" -d "{\"idempotency_key\":\"s67-pub-2-$STAMP\",\"action\":\"publish\",\"reason\":\"s6\"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is True, d
assert d['data']['status']=='published', d['data']
print('PASS: fully-priced pass published')
"

echo
echo "ALL LIVE EVIDENCE PASSED (BI S6)"
