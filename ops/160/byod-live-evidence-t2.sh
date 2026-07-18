#!/usr/bin/env bash
# BYOD 一期活证据:员工自有手机四功能全链(staff_private_phone 终端头)——
# 注册申请→审批;公告读+已读留痕;我的工单只见自己;酒吧空位→员工代订(免定金)→占用标记。
# APK 产物:apps/android-terminal/android-app gradle assembleDebug(sha256 见脚本输出)。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
TH='X-Terminal-Type: staff_private_phone'
PG=postgresql://complex:${COMPLEX_DATABASE_PASSWORD:-complex_dev_pwd}@127.0.0.1:5432/complex

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"
ME=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["user"]["id"])')
echo "me=$ME"

echo "== 1. 手机提交员工注册申请 → 审批通过 =="
REG=$(curl -s -X POST $API/api/v1/staff/registrations -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"byod-reg-$STAMP\",\"project_id\":\"project_1\",\"name\":\"BYOD演练员工$STAMP\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:200];print(d["data"]["id"])')
curl -s -X POST $API/api/v1/staff/registrations/$REG/review -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-rev-$STAMP\",\"decision\":\"approve\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
print("PASS: registration submitted on phone & approved, status =", d["data"].get("status"))'

echo "== 2. 公告:后台已发布的公告(种一条演练件)→ 手机可见 → 一键已读留痕 =="
psql $PG -c "INSERT INTO employee_notices (id, staff_profile_id, notice_type, title, body, consent_required, consent_status, created_by, created_at, updated_at) VALUES ('ntc_byod_$STAMP', '$REG', 'system_notice', 'BYOD演练公告', '本月安全培训周四下午', false, 'pending', 'admin', now(), now());" > /dev/null
curl -s "$API/api/v1/staff/notices" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['id']=='ntc_byod_$STAMP']
assert mine, 'notice missing'
assert mine[0]['title']=='BYOD演练公告', mine[0]
print('PASS: notice visible on phone')
"
curl -s -X POST "$API/api/v1/staff/notices/ntc_byod_$STAMP/acknowledge" -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"byod-ack-$STAMP\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
print("PASS: acknowledged, consent_status =", d["data"].get("consent_status"))'

echo "== 3. 我的工单:两单各派不同人 → 手机只见派给自己的 =="
WO1=$(curl -s -X POST $API/api/v1/facility/work-orders -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-wo1-$STAMP\",\"work_order_type\":\"repair\",\"priority\":\"normal\",\"title\":\"BYOD我的工单$STAMP\",\"source\":\"platform\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
WO2=$(curl -s -X POST $API/api/v1/facility/work-orders -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-wo2-$STAMP\",\"work_order_type\":\"cleaning\",\"priority\":\"normal\",\"title\":\"BYOD别人的工单$STAMP\",\"source\":\"platform\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["id"])')
curl -s -X POST $API/api/v1/facility/work-orders/$WO1/assign -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-as1-$STAMP\",\"assignee_id\":\"$ME\"}" > /dev/null
curl -s -X POST $API/api/v1/facility/work-orders/$WO2/assign -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-as2-$STAMP\",\"assignee_id\":\"usr_someone_else\"}" > /dev/null
curl -s "$API/api/v1/facility/work-orders/assigned-to-me" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
ids=[r['id'] for r in rows]
assert '$WO1' in ids, 'my order missing'
assert '$WO2' not in ids, 'someone else order leaked!'
print('PASS: assigned-to-me shows mine only (%d rows)' % len(rows))
"

echo "== 4. 酒吧空位 → 员工代订(免定金) → 占用标记翻转 =="
TBL=$(curl -s -X POST $API/api/v1/bar/tables -H "$J" -H "$AH" -d "{\"idempotency_key\":\"byod-tbl-$STAMP\",\"bar_id\":\"bar_1\",\"table_name\":\"BYOD演练卡座$STAMP\",\"table_type\":\"card_seat\",\"min_consumption_cent\":50000,\"capacity\":4,\"status\":\"active\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:300];print(d["data"]["id"])')
curl -s "$API/api/v1/bar/tables/availability" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['id']=='$TBL']
assert mine and mine[0]['reserved'] is False, mine
print('PASS: new table listed as free')
"
AT=$(date -u -d '+3 hours' +%Y-%m-%dT%H:%M:%SZ)
curl -s -X POST $API/api/v1/bar/reservations -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"byod-resv-$STAMP\",\"bar_id\":\"bar_1\",\"table_id\":\"$TBL\",\"reservation_time\":\"$AT\",\"guest_count\":4,\"salesperson_user_id\":\"$ME\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
dep=d["data"].get("deposit_status")
assert dep in (None, "not_required"), dep   # 员工代订免定金
print("PASS: reservation created by staff phone, deposit =", dep)'
curl -s "$API/api/v1/bar/tables/availability" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['id']=='$TBL']
assert mine and mine[0]['reserved'] is True, mine
print('PASS: table now marked reserved — 空位视图与预订同口径')
"

echo
echo "== APK 产物 =="
sha256sum ~/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk
echo
echo "ALL LIVE EVIDENCE PASSED (BYOD T2)"
