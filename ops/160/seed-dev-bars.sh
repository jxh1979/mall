#!/usr/bin/env bash
# 开发环境:按「招商 / 商户准入」正路建 N 家酒吧商户(酒吧A98、酒吧B98 …)。
#
# 为什么是脚本不是迁移:酒吧＝商户(merchant_type='bar'),不是经营主体;建商户是招商的业务动作,
#   走后台正路才有审批与留痕。把 10 家写进 Flyway 迁移链会把开发测试数据固化进正式发布链,
#   而且用户明确「酒吧数量必须可配 —— 可能七八家,也可能二三十家」,写死家数正好违背这条。
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

# 娱乐平台公司参股各酒吧(业务口径),故所属经营主体挂娱乐平台公司(V354 稳定 id)。
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
