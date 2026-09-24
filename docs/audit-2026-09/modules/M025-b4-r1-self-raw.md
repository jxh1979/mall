CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T22:48:18Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M025 B3-r1 修复 diff：before tree `ebddcf665b30608535e956671f42bbe4e3ca49de` → after tree `0395a637691b73a32a1eb1e81642cfc0313edbb5`；冻结 diff `/home/james/audit-tmp/m025_b3_r1.diff`（1759 bytes，SHA256 d53572e13e25b6a9baefa8641d6f14f15f87279dad8e66070fc38bfed5ac9e20），全文见 §2。定稿 ID：M025-001、002（队列「是」）；GR-03 只登记。
- 顺序：Grok 已先派（modules/M025-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。未运行被审脚本、未调用接口。

## 1. 逐行归因（§5 闸 3）

| ± | 归因 ID | 行首 80 字 |
|---|---|---|
| `+` | 版本行(001/002同批) | # 文件版本：V0.01 |
| `-` | 002 | # 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。 |
| `+` | 002 | # 开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。 |
| `-` | 001 |     ¦ python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items |
| `+` | 001 |     ¦ python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items |

- 1 文件 3 个 hunk、5 条 ± 行全部命中 001／002／随批版本行；无归因不到的行；diff 无 mode 变更行（可执行位保留）；未改其他文件；GR-03 未混入（无预检、无对项目范围规则的描述）。归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/ops/160/seed-dev-bars.sh b/ops/160/seed-dev-bars.sh
index 9a8be0c6..24dc5aee 100755
--- a/ops/160/seed-dev-bars.sh
+++ b/ops/160/seed-dev-bars.sh
@@ -1,4 +1,5 @@
 #!/usr/bin/env bash
+# 文件版本：V0.01
 # 开发环境:用后台建商户接口播 N 家酒吧商户(酒吧A98、酒吧B98 …),供联调。
 # 本脚本不替代招商、合同、准入审批,也不把测试数据写进正式迁移。
 #
@@ -27,7 +28,7 @@ if [ -z "${ADMIN_TOK:-}" ]; then
 fi
 AH="Authorization: Bearer $ADMIN_TOK"
 
-# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。
+# 开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。
 ENTITY=be_ent_platform
 
 echo "== 建 $COUNT 家酒吧商户(所属主体 $ENTITY)=="
@@ -35,7 +36,7 @@ for i in $(seq 0 $((COUNT - 1))); do
   if [ "$i" -lt 26 ]; then SUFFIX="${LETTERS:$i:1}"; else SUFFIX="$((i + 1))"; fi
   NAME="酒吧${SUFFIX}98"
   EXISTS=$(curl -s "$API/api/v1/platform/merchants?page=1&size=500" -H "$AH" \
-    | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('merchantName')=='$NAME'),''))")
+    | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('merchant_name')=='$NAME'),''))")
   if [ -n "$EXISTS" ]; then echo "  跳过(已存在) $NAME=$EXISTS"; continue; fi
   ID=$(curl -s -X POST "$API/api/v1/platform/merchants" -H "$J" -H "$AH" -d "{
     \"idempotency_key\":\"seed-bar-${SUFFIX}98\",
```

## 3. 内容审计

- 001：L38 由 `m.get('merchantName')` 改 `m.get('merchant_name')`，与后端 SNAKE_CASE 响应一致；头注 L10「按 merchant_name 查」现与实现一致；幂等键、请求体、接口路径未动。
- 002：注释前半句改为「给商户填一个已有经营主体 id(接口不强制该字段)」，与 `MerchantProfileRequest.businessEntityId` 可空一致；后半句「不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体」逐字保留；仍传该字段。
- 版本行：第 2 行 `# 文件版本：V0.01`，与 `ops/160/merge-pr.sh` 等同款位置。
- 修前/修后自检原文：
```
[RED]
lookup expression: import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('me…
EXISTS=[] stderr=[]
RED: 已存在的「酒吧A98」查不到，脚本会再次创建
RED: 注释仍称 business_entity_id「接口必填」
文件版本行: 无
problems=2
[GREEN]
lookup expression: import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('me…
EXISTS=[mch_1] stderr=[]
文件版本行: ['# 文件版本：V0.01']
problems=0
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m025_b3r1_run.log）

```
START=2026-09-20T22:48:06Z HEAD=e456ba96
check OK 3 subs mode 0o775
BEFORE_TREE=ebddcf665b30608535e956671f42bbe4e3ca49de
== RED: self-check against old script
RED_CHECK_EXIT=1
lookup expression: import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('me…
EXISTS=[] stderr=[]
RED: 已存在的「酒吧A98」查不到，脚本会再次创建
RED: 注释仍称 business_entity_id「接口必填」
文件版本行: 无
problems=2
== apply
wrote seed-dev-bars.sh b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331 lines 56 mode 0o775
== GREEN: same self-check + bash -n
GREEN_CHECK_EXIT=0
lookup expression: import sys,json;d=json.load(sys.stdin)['data'];items=d['items'] if isinstance(d,dict) else d;print(next((m['id'] for m in items if m.get('me…
EXISTS=[mch_1] stderr=[]
文件版本行: ['# 文件版本：V0.01']
problems=0
BASH_N_EXIT=0
mode=775 index-mode-after-add=100755
AFTER_TREE=0395a637691b73a32a1eb1e81642cfc0313edbb5
other files touched: 0
DIFF bytes=1759 sha256=d53572e13e25b6a9baefa8641d6f14f15f87279dad8e66070fc38bfed5ac9e20
 ops/160/seed-dev-bars.sh | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)
hunks=3 files=1
ops/160/seed-dev-bars.sh 56 2685 b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T22:48:07Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
