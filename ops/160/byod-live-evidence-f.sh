#!/usr/bin/env bash
# BYOD F批活证据:薪资四块→月结定格→员工手机可见(恒等式)→未结月为空→请假全流转→月视图六态。
set -euo pipefail
API=http://127.0.0.1:8080
STAMP=$(date +%s)
J='Content-Type: application/json'
TH='X-Terminal-Type: staff_private_phone'

TOK=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])')
AH="Authorization: Bearer $TOK"
ME=$(curl -s -X POST $API/api/v1/auth/login -H "$J" -d '{"login_name":"admin","password":"admin-pass"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["user"]["id"])')
LAST_MONTH=$(date -u -d 'last month' +%Y-%m)
THIS_MONTH=$(date -u +%Y-%m)

echo "== 1. 人事设固定工资 8000 元 + 录提成 1200 + 奖金 500(带事由) + 处罚 200(带事由) =="
curl -s -X POST $API/api/v1/staff/payroll/base -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-base-$STAMP\",\"user_id\":\"$ME\",\"monthly_salary_cent\":800000}" > /dev/null
curl -s -X POST $API/api/v1/staff/payroll/commissions -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-cm-$STAMP\",\"user_id\":\"$ME\",\"month\":\"$LAST_MONTH\",\"source_type\":\"manual\",\"amount_cent\":120000,\"note\":\"订位提成\"}" > /dev/null
curl -s -X POST $API/api/v1/staff/payroll/adjustments -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-bn-$STAMP\",\"user_id\":\"$ME\",\"month\":\"$LAST_MONTH\",\"adj_type\":\"bonus\",\"amount_cent\":50000,\"reason\":\"月度之星\"}" > /dev/null
curl -s -X POST $API/api/v1/staff/payroll/adjustments -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-pn-$STAMP\",\"user_id\":\"$ME\",\"month\":\"$LAST_MONTH\",\"adj_type\":\"penalty\",\"amount_cent\":20000,\"reason\":\"迟到3次\"}" > /dev/null
echo "recorded"

echo "== 2. 无事由的处罚必须被拒(钱路留痕硬约束) =="
curl -s -X POST $API/api/v1/staff/payroll/adjustments -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-bad-$STAMP\",\"user_id\":\"$ME\",\"month\":\"$LAST_MONTH\",\"adj_type\":\"penalty\",\"amount_cent\":100,\"reason\":\" \"}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
assert d.get('success') is False, str(d)[:200]
print('PASS: adjustment without reason rejected')
"

PG=postgresql://complex:complex_dev_pwd@127.0.0.1:5432/complex
# 重放清场:上轮活证据的定格单属测试数据,删掉以验证「月结前不可见」。
psql $PG -c "DELETE FROM staff_payroll_statements WHERE user_id='$ME' AND month='$LAST_MONTH';" > /dev/null

echo "== 3. 月结前员工手机查 → 空(拍板:月结后才可见) =="
curl -s "$API/api/v1/staff/payroll/mine?month=$LAST_MONTH" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=[r for r in json.load(sys.stdin)['data']['items'] if r['user_id']=='$ME']
assert not rows, rows
print('PASS: invisible before month close')
"

echo "== 4. 月结 → 员工手机可见四块+恒等式 total=8000+1200+500-200=9500 元 =="
curl -s -X POST $API/api/v1/staff/payroll/close -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-close-$STAMP\",\"month\":\"$LAST_MONTH\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:200];print("closed, statements =", d["data"]["statements"])'
curl -s "$API/api/v1/staff/payroll/mine?month=$LAST_MONTH" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=[r for r in json.load(sys.stdin)['data']['items'] if r['user_id']=='$ME']
assert rows, 'statement missing'
r=rows[0]
assert r['base_cent']==800000 and r['commission_cent']>=120000, r
assert r['bonus_cent']>=50000 and r['penalty_cent']>=20000, r   # 台账重放累积,下限断言
assert r['total_cent']==r['base_cent']+r['commission_cent']+r['bonus_cent']-r['penalty_cent'], r
print('PASS: statement visible, total=%s cent (恒等式成立)' % r['total_cent'])
"

echo "== 5. 请假全流转:手机提交年假 → 主管通过 → 我的请假可见状态 =="
LV=$(curl -s -X POST $API/api/v1/staff/leaves -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"f-lv-$STAMP\",\"leave_type\":\"annual\",\"start_date\":\"$THIS_MONTH-25\",\"end_date\":\"$THIS_MONTH-26\",\"reason\":\"年假\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d.get("success"),str(d)[:300];print(d["data"]["id"])')
curl -s -X POST "$API/api/v1/staff/leaves/$LV/review" -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-rv-$STAMP\",\"decision\":\"approve\",\"review_note\":\"准\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d["data"]["status"]=="approved",d["data"];print("PASS: leave approved by supervisor")'
curl -s "$API/api/v1/staff/leaves/mine" -H "$AH" -H "$TH" | python3 -c "
import sys,json
rows=[r for r in json.load(sys.stdin)['data']['items'] if r['id']=='$LV']
assert rows and rows[0]['status']=='approved', rows
print('PASS: my leaves shows approved')
"

echo "== 6. 自包含排班+打卡:今天排班(已开始2小时)→上班卡判迟到 =="
START=$(date -u -d '-2 hours' +%Y-%m-%dT%H:%M:00Z)
END=$(date -u -d '+6 hours' +%Y-%m-%dT%H:%M:00Z)
curl -s -X POST $API/api/v1/staff/shifts -H "$J" -H "$AH" -d "{\"idempotency_key\":\"f-shf-$STAMP\",\"user_id\":\"$ME\",\"work_date\":\"$(date -u +%F)\",\"start_at\":\"$START\",\"end_at\":\"$END\",\"point_space_ids\":[\"sp_f_point\"]}" > /dev/null
curl -s -X POST $API/api/v1/staff/attendance/punch -H "$J" -H "$AH" -H "$TH" -d "{\"idempotency_key\":\"f-pi-$STAMP\",\"punch_type\":\"check_in\",\"method\":\"nfc\",\"tag_ref\":\"space:sp_f_point\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);assert d["data"]["judgement"]=="late",d["data"];print("PASS: check-in judged late")'

echo "== 7. 考勤月视图六态:请假日=leave;今天=迟到(+未打下班卡);无排班=rest =="
curl -s "$API/api/v1/staff/attendance/month-view?month=$THIS_MONTH" -H "$AH" -H "$TH" | python3 -c "
import sys,json
days={d['date']:d['day_status'] for d in json.load(sys.stdin)['data']}
assert days.get('$THIS_MONTH-25')==['leave'], days.get('$THIS_MONTH-25')
assert days.get('$THIS_MONTH-26')==['leave'], days.get('$THIS_MONTH-26')
today_status=days.get('$(date -u +%F)')
assert today_status and 'late' in today_status and 'missing_checkout' in today_status, today_status
rest=[v for v in days.values() if v==['rest']]
assert rest, 'no rest days?'
print('PASS: month view — leave days marked, today=%s, rest days=%d' % (today_status, len(rest)))
"

echo
echo "== APK 产物 =="
sha256sum ~/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk
echo
echo "ALL LIVE EVIDENCE PASSED (BYOD F批)"
