#!/usr/bin/env bash
# 开发环境:用后台建商户接口播 N 家酒吧商户(酒吧A98、酒吧B98 …),供联调。
# 本脚本不替代招商、合同、准入审批,也不把测试数据写进正式迁移。
#
# 为什么是脚本不是迁移:把固定家数写进 Flyway 会把开发数据固化进发布链;
#   酒吧家数必须可配(七八家到二三十家),写死家数正好违背这条。
#
# 家数是参数:  ./seed-dev-bars.sh          → 10 家(A98…J98,与 2026-07 主数据一致)
#              ./seed-dev-bars.sh 25       → 25 家
# 幂等:同名商户已存在则跳过(按 merchant_name 查)。
set -euo pipefail

COUNT="${1:-10}"
API=${API:-http://127.0.0.1:8080}
J='Content-Type: application/json'
LETTERS=ABCDEFGHIJKLMNOPQRSTUVWXYZ

tok() { curl -s -X POST $API/api/v1/auth/login -H "$J" \
  -d "{\"login_name\":\"$1\",\"password\":\"$2\"}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["access_token"])'; }

ADMIN_TOK=$(tok admin admin-pass 2>/dev/null || true)
if [ -z "${ADMIN_TOK:-}" ]; then
  P0U=$(grep '^P0_ADMIN_USERNAME=' ~/.complex/complex.env | cut -d= -f2)
  P0P=$(grep '^P0_ADMIN_PASSWORD=' ~/.complex/complex.env | cut -d= -f2)
  ADMIN_TOK=$(tok "$P0U" "$P0P")
fi
AH="Authorization: Bearer $ADMIN_TOK"

# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。
ENTITY=be_ent_platform

echo "== 建 $COUNT 家酒吧商户(所属主体 $ENTITY)=="
for i in $(seq 0 $((COUNT - 1))); do
  if [ "$i" -lt 26 ]; then SUFFIX="${LETTERS:$i:1}"; else SUFFIX="$((i + 1))"; fi
  NAME="酒吧${SUFFIX}98"
  EXISTS=$(curl -s "$API/api/v1/platform/merchants?page=1&size=500" -H "$AH" \
    | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('merchantName')=='$NAME'),''))")
  if [ -n "$EXISTS" ]; then echo "  跳过(已存在) $NAME=$EXISTS"; continue; fi
  ID=$(curl -s -X POST "$API/api/v1/platform/merchants" -H "$J" -H "$AH" -d "{
    \"idempotency_key\":\"seed-bar-${SUFFIX}98\",
    \"business_entity_id\":\"$ENTITY\",
    \"merchant_name\":\"$NAME\",
    \"merchant_type\":\"bar\",
    \"contact_name\":\"$NAME 负责人\",
    \"status\":\"active\",
    \"operator_note\":\"开发环境酒吧主数据播种(家数可配)\"
  }" | python3 -c 'import sys,json
raw=sys.stdin.read()
try: print(json.loads(raw)["data"]["id"])
except Exception:
    sys.stderr.write("RESPONSE: "+raw[:400]+"\n"); raise SystemExit(1)')
  echo "  建成 $NAME=$ID"
done
echo "== 完成:$COUNT 家 =="
