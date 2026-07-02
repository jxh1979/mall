#!/usr/bin/env bash
# BYOD E批活证据:主管排班(双端)→员工查班次点位→NFC语义打卡严格判定→后台考勤;APK产物。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
TH='X-Terminal-Type: staff_private_phone'

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"
ME=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["user"]["id"])')
TODAY=$(date -u +%F)
START=$(date -u -d '-1 hour' +%Y-%m-%dT%H:%M:00Z)
END=$(date -u -d '+7 hours' +%Y-%m-%dT%H:%M:00Z)

echo "== 1. 主管在手机上排班(到分钟+两个点位) =="
curl -s -X POST $API/api/v1/staff/shifts -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"e-shf-$STAMP\",\"user_id\":\"$ME\",\"work_date\":\"$TODAY\",\"start_at\":\"$START\",\"end_at\":\"$END\",\"point_space_ids\":[\"sp_point_a\",\"sp_point_b\"],\"note\":\"E批活证据\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
assert d["data"]["point_space_ids"]==["sp_point_a","sp_point_b"], d["data"]
print("PASS: shift saved with minute precision & two points")'

echo "== 2. 幂等覆盖:同员工同开始时刻改点位 → 仍单班次,点位更新 =="
curl -s -X POST $API/api/v1/staff/shifts -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"e-shf2-$STAMP\",\"user_id\":\"$ME\",\"work_date\":\"$TODAY\",\"start_at\":\"$START\",\"end_at\":\"$END\",\"point_space_ids\":[\"sp_point_c\"],\"note\":\"改点位\"}" > /dev/null
curl -s "$API/api/v1/staff/shifts?date=$TODAY&user_id=$ME" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r['start_at'].startswith('${START%Z}'[:16])]
assert len(mine)==1, ('expect single shift', len(mine))
assert mine[0]['point_space_ids']==['sp_point_c'], mine[0]
print('PASS: same (user,start) overwritten in place — 单行覆盖')
"

echo "== 3. 员工手机查我的班次与点位 =="
curl -s "$API/api/v1/staff/shifts/mine" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
assert any(r.get('note')=='改点位' for r in rows), rows[:2]
print('PASS: my shifts visible on phone (%d rows, 7-day window)' % len(rows))
"

echo "== 4. NFC 语义打卡:班次已开始 1 小时 → 上班打卡判「迟到」(严格无宽限) =="
curl -s -X POST $API/api/v1/staff/attendance/punch -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"e-pin-$STAMP\",\"punch_type\":\"check_in\",\"method\":\"nfc\",\"tag_ref\":\"space:sp_point_c\",\"space_id\":\"sp_point_c\",\"device_fingerprint\":\"fp_e_evidence\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
assert d["data"]["judgement"]=="late", d["data"]
assert d["data"]["method"]=="nfc" and d["data"]["tag_ref"]=="space:sp_point_c", d["data"]
print("PASS: check-in judged LATE (server time, no grace)")'

echo "== 5. 班次未结束 → 下班打卡判「早退」 =="
curl -s -X POST $API/api/v1/staff/attendance/punch -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"e-pout-$STAMP\",\"punch_type\":\"check_out\",\"method\":\"qr\",\"tag_ref\":\"space:sp_point_c\",\"space_id\":\"sp_point_c\",\"device_fingerprint\":\"fp_e_evidence\"}" | python3 -c '
import sys,json
d=json.load(sys.stdin)
assert d.get("success"), str(d)[:300]
assert d["data"]["judgement"]=="early_leave", d["data"]
print("PASS: check-out judged EARLY_LEAVE")'

echo "== 6. 后台按日查考勤:两条记录含判定/方式/设备指纹留痕 =="
curl -s "$API/api/v1/staff/attendance?date=$TODAY" -H "$AH" | python3 -c "
import sys,json
rows=json.load(sys.stdin)['data']['items']
mine=[r for r in rows if r.get('device_fingerprint')=='fp_e_evidence']
assert len(mine)>=2, len(mine)
judges={r['judgement'] for r in mine}
assert {'late','early_leave'}.issubset(judges), judges
print('PASS: attendance query shows %d punches with strict judgements' % len(mine))
"

echo
echo "== APK 产物 =="
sha256sum ~/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk
echo
echo "ALL LIVE EVIDENCE PASSED (BYOD E批)"
