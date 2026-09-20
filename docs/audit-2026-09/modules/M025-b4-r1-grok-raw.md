CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M025 B4-r1 Grok 席乙只读审计（修复 diff）
版本：V0.01
席位：Grok 独立只读审计席乙（grok-4.6 / xhigh）
步骤：B4-r1（160-audit-prompt.md V0.17 §4 B4 + §5 闸 3）
正本：`/home/james/complex-management` main 主工作区
对象：席甲 B3-r1 整批修复 diff（运行和发布脚本模块）
findings 正本：`docs/audit-2026-09/findings.md` V2.81，已定稿且归属「是」：M025-001、M025-002
独立性：本席未读 `M025-b4-r1-self-raw.md`（该路径存在，本会话未打开）；未派 MCP / 子代理 / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy；未运行 `ops/160/seed-dev-bars.sh`；未调用任何接口。旧 B1/B2 可对照（`M025-b1-grok-raw.md` V0.02 B2-r1 段、findings V2.81）；本轮以 V2.81 定稿最小处置为准。过程中曾把 before/after blob 重定向到 `/tmp` 做 `bash -n`，随即 `rm`；不采该副本为证据。可执行位与语法以 git tree mode + 工作区 `bash -n` 为准。未留临时产物。

---

## 0. 技能预检（§6，按本环境实核）

核到且按收窄口径使用：
1. `using-superpowers`：开工技能自检。
2. `using-arc`：已读；其默认「派评审子代理」与双席铁律冲突，本席不走 Arc 审计流程。
3. `graft`：定位 `merchant_name`（Jackson 出网键 / `JdbcPlatformRepository` 列名 / `PlatformService.saveMerchant` 的 `"merchant_name"` 字段标签）与 `MerchantProfileRequest.businessEntityId` / `PlatformService.saveMerchant:1011`。两呼约节省 302,811 tokens。
4. `grilling` / `grill-me`：对抗式自问自答；本体「等用户 / 派子代理」作废。`grill-me` 本体只转发 grilling。
5. `ponytail-review`：只找可删的多余抽象；不改码。结论：Lean already. Ship.
6. `verification-before-completion`：PASS 前当场复跑只读自检、独立红/绿表达式、`bash -n`、两树 `git diff --binary` 与冻结 diff `cmp`、POST 体逐字比对。
7. `systematic-debugging` / `diagnosing-bugs`：只借读错误→复现→追数据流。红环=`python3 /home/james/audit-tmp/m025_selfcheck.py`（既有，本席不新建）+ 对 before 键名喂蛇形样例。本体 instrumentation / 最小改码试假说作废。本步不修。
8. `tdd` / `test-driven-development`：本 diff 是脚本查重键名与一句注释；定稿验证面是「同款 python 表达式对后端形状蛇形样例」，不强制落测试文件。加测试文件会改他文件，闸 3 越界。
9. `requesting-code-review`：B4＝本席即外审；不用 HEAD~1、不用子代理复审。
10. `codebase-design`：出网缝是全局 SNAKE_CASE JSON；Java 记录字段 `merchantName` / `businessEntityId` 是实现名，不是列表响应键。001 修的是出网缝上的键。
11. `writing-for-agents`：本 raw 给下游席读。
12. `i-have-adhd`：编号、无寒暄；本文件第一行仍必须是判据锁哈希（提示词覆盖输出模式）。
13. `long-running-background-tasks`：已读；本步无长任务。

明确不用（提示词划掉或本步不适用）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`executing-plans`、`research`、`ponytail-audit`、mattpocock `code-review`。`brainstorming` / `writing-plans` / `ponytail` 本体属 A/B3，本步只读不修。`receiving-code-review` 属席甲收票。`resolving-merge-conflicts` 无合并。

缺口（实找无目录，不冒充已调用）：`batch-grill-me`、`ubiquitous-language`。
可选存在未用：`git-guardrails-claude-code`（本席只读、无 git 写；本体要问人装 hook，第四闸）。

---

## 1. 输入身份（类型 4）

本席独立实算（未从 sidecar / ledger / 派发词抄写）：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```
sidecar 与 ledger 顶部同值，仅作旁证，不以它们代替实算。

冻结 diff：
```
wc -c /home/james/audit-tmp/m025_b3_r1.diff → 1759
sha256sum → d53572e13e25b6a9baefa8641d6f14f15f87279dad8e66070fc38bfed5ac9e20
```
与派发声明一致。

tree（`git cat-file -t` 均为 tree）：
- before `ebddcf665b30608535e956671f42bbe4e3ca49de`
- after  `0395a637691b73a32a1eb1e81642cfc0313edbb5`

```
git diff --binary <before> <after> | sha256sum
→ d53572e13e25b6a9baefa8641d6f14f15f87279dad8e66070fc38bfed5ac9e20
git diff --binary <before> <after> | cmp - /home/james/audit-tmp/m025_b3_r1.diff → CMP_OK
git diff --stat：1 file changed, 3 insertions(+), 2 deletions(-)
git diff --name-status：仅 M ops/160/seed-dev-bars.sh
hunks=3（冻结 diff 3 个 @@）
```

blob / mode：
- before `9a8be0c69749279d0d3f077bda768c5180c291e8` mode `100755` 内容 SHA256 `bf65ae73f92fb105d848d866212694bcd7daf3c9a284c9630713910f10123044`（55 行；与本席 B1 grok-raw 对象 SHA 一致）
- after  `24dc5aeeebfc7f38e1c4587ea0032061ce7a8d2c` mode `100755` 内容 SHA256 `b44ad5d2614166a4a1aee5617ef46b5efebf7944564f4da7819aafe2f0e3c331`（56 行 / 2685 bytes）
- 工作区 `git hash-object ops/160/seed-dev-bars.sh` = after blob；`cmp` 工作区与 after blob 相同。`ls` mode `775`（git 记 `100755`，组写位是工作区 umask，两树 mode 未变）。分支 `main`，`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`。

闸 3：diff＝两 tree 之间全部差异；每一 `+`/`-` 行须归到 V2.81 归属「是」的 ID；归不到即越界。

模块闭集：`plan/modules.md` M025 FILES 仅 `ops/160/seed-dev-bars.sh`（`scope.txt` 第 2852 行）。`git diff --name-only` 闭集＝该 1 文件。

---

## 2. 定稿最小处置（审计标尺，摘 V2.81）

| ID | 分类 | 修复队列 | 最小处置 |
|---|---|---|---|
| M025-001 | 细化 | 是 | L38 键名改 `merchant_name`（一处）；文件第 2 行加 `# 文件版本：V0.01`。不改接口、不改幂等键、不加功能。验证（不调用接口）：脚本同款 python 表达式对后端形状的蛇形样例，修前输出空、修后输出该商户 id |
| M025-002 | 细化 | 是 | 只改这一句注释的前半——「开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。」后半「不表示…」原样保留；不改是否传该字段 |

B2 双席已同意版本行同批：本席 B1 grok-raw V0.02「文件第 2 行加 `# 文件版本：V0.01`。属最小处置…B3 在 shebang 之后插入新行，不得覆盖现 L2」；findings V2.81 001 最小处置写明同一句。无第三 ID。不是无 ID 新缺陷。

只登记不入队（不得混进本 diff）：GR-03（创建主路径依赖当前项目与 `business_entity_projects`；挂 M009 未发布改动联动；不得加预检、不得写入对未发布规则的描述）；头注 L8「与 2026-07 主数据一致」；L22 开发默认口令试登；口令进进程列表；查重只取第一页 500；origin/main `status=active` / PR #445；`requireIdempotencyKey` 英文公共类。本 diff 未碰这些。

---

## 3. diff 全文（冻结件 1759 bytes，与 `git diff --binary` 字节级相同）

```
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

index 行两侧均为 `100755`：可执行位保留，无 mode hunk。

---

## 4. diff 逐块归因（闸 3）

`+`/`-` 行闭集＝3 插入 + 2 删除。上下文行无 `+`/`-`，不入归因。

### 4.1 H01 `@@ -1,4 +1,5`

| 行 | 内容 | ID |
|---|---|---|
| `+# 文件版本：V0.01` | shebang 下一行插入版本标记；未覆盖原 L2 头注（原 L2 现为 after L3，逐字相同） | M025-001 |

上下文 `#!/usr/bin/env bash` 与原 L2/L3 头注无 `+`/`-`。

### 4.2 H02 `@@ -27,7 +28,7`

| 行 | 内容 | ID |
|---|---|---|
| `-# 开发播种挂一个已有经营主体,只为接口必填;不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。` / `+# 开发播种给商户填一个已有经营主体 id(接口不强制该字段);不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。` | 只改前半；后半「不表示各酒吧生产上同一经营主体,也不表示参股等于经营主体。」原样 | M025-002 |

上下文 `ENTITY=be_ent_platform` 无 `+`/`-`。

### 4.3 H03 `@@ -35,7 +36,7`

| 行 | 内容 | ID |
|---|---|---|
| `- … m.get('merchantName') …` / `+ … m.get('merchant_name') …` | 查重键名一处；同一 python 表达式其余字符不变 | M025-001 |

上下文 POST 起句 `\"idempotency_key\":\"seed-bar-${SUFFIX}98\",` 无 `+`/`-`。H03 截断于幂等键行，请求体其余字段不在变更集。

### 4.4 归因结论

3 插入、2 删除均可落到 M025-001 / M025-002（版本行归 001，B2 双席同意同批）。  
未见「顺手优化 / 加预检 / 改幂等键 / 改请求体 / 写未发布规则 / 改其他文件 / 丢可执行位」。  
`git diff --name-only` 闭集＝1 文件。  
before→after 除插入行外的行映射：仅 before L30↔after L31（002）与 before L38↔after L39（001）不同；其余 53 行逐字相同。  
**归因票：PASS。**

---

## 5. 内容审计

### 5.1 三线（本步范围＝该 diff，不是整模块重审）

**第一性原理**
1. 删掉这些改写，V2.81 两条「是」仍红：查重键 `merchantName` 对蛇形列表永不命中；注释仍称「接口必填」。必须存在。
2. 声称的现行事实：下列命令/源码已核（类型 4）。
3. 未验证前提：无。本席未跑播种脚本、未调接口；查重对错用抽出的表达式 + 蛇形样例证明。

**顺从式**  
假定 B3 意图正确，逐 ID 对照最小处置原文与 after 树对应句，见 5.3。

**对抗式（grilling 自答）**
- Q：版本行无独立 ID，是否越界？→ 否。V2.81 001 最小处置明文「文件第 2 行加 `# 文件版本：V0.01`」；B2-r1 本席已同意「插入、不得覆盖现 L2」。同目录 `ops/160/merge-pr.sh:2` 已是 `# 文件版本：V0.02`。after L2 恰为 `# 文件版本：V0.01`，after L3＝before L2。
- Q：会不会加了 `or m.get('merchantName')` 双键？→ after 对 `merchantName` 0 命中；`m.get('merchant_name')` 恰 1 处。
- Q：请求体 / 幂等键是否被 hunk 上下文牵动？→ POST 块（`ID=$(curl -s -X POST` 至 `建成`）before/after SHA 相同。字段仍是 `idempotency_key=seed-bar-${SUFFIX}98`、`business_entity_id=$ENTITY`、`merchant_name/$NAME`、`merchant_type=bar`、`contact_name`、`status=pending_admission`、`operator_note`。
- Q：002 是否改了后半或是否传字段？→ 后半子串在 before 与 after 均存在且注释整句与定稿拟改逐字相同。`ENTITY=be_ent_platform` 仍 1 处；仍 POST `business_entity_id`。
- Q：GR-03 是否混入（预检 / 项目规则注释）？→ after 对 `creationProject` / `businessEntityInProject` / `PROJECT_SCOPE_MISMATCH` / `预检` / `business_entity_projects` 均为 0。未新增 curl/分支。
- Q：Java 字段叫 `merchantName`，改 JSON 键是否改错缝？→ `application.yml:16` `property-naming-strategy: SNAKE_CASE`；`PlatformControllerTests.java:150` 断言 `$.data.items[0].merchant_name`。列表出网键是蛇形。改脚本读取键对齐出网缝，不改 Java 字段名、不改接口。
- Q：`businessEntityId` 现行是否仍可空？→ `MerchantProfileRequest.java` 无 `@NotNull`/`@NotBlank`/`jakarta.validation`。主工作区 `PlatformService.java:1011` 仅 `trimToNull(...) != null` 才 `requireBusinessEntity`。origin/main `:874` 创建路径 `trimToNull(request.businessEntityId())` 可空写入、不强制。注释「接口不强制该字段」与两态一致。
- Q：可执行位 / 语法？→ 两树 `100755`；工作区 `bash -n` exit 0。
- Q：加测试文件或改 `modules.md` 行数 55→56？→ 未改。加他文件即越界。

### 5.2 与现行后端（只读，不调接口）

**001 查重键 vs SNAKE_CASE**
- `backend/src/main/resources/application.yml:15-16`：`spring.jackson.property-naming-strategy: SNAKE_CASE`。
- `PlatformControllerTests.java:145-150`：`GET /api/v1/platform/merchants` 断言 `$.data.items[0].merchant_name`（不是 `merchantName`）。
- `PlatformService.java:998`：`requireText(..., "merchant_name")` 字段标签已是蛇形。
- `JdbcPlatformRepository` 列名 `merchant_name`（graft 多处 INSERT/SELECT）。
- after L39 `m.get('merchant_name')` 与上述出网键一致。before L38 `m.get('merchantName')` 对同一蛇形样例得到空串。

**002 注释 vs 可空字段**
- `MerchantProfileRequest.java:7-18`：`String businessEntityId` 无必填注解（主工作区仅多未发布 `projectId`，不改变本字段可空）。
- origin/main 同记录亦无必填注解。
- 主工作区 `PlatformService.java:1011`：`if (trimToNull(request.businessEntityId()) != null) requireBusinessEntity(...)`。
- origin/main `:874`：`existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId()`。
- after L31 前半「接口不强制该字段」与两态可空一致；脚本仍传该字段，符合「不改是否传」。

### 5.3 按 ID 核最小处置

**001** after L39 仅把 `merchantName` 改为 `merchant_name`（一处）；after L2 `# 文件版本：V0.01`；未改接口、幂等键、UNIQUE、双键、分页 size。通过。

**002** after L31 整句与定稿拟改逐字相同；后半原样；`ENTITY` 与 POST `business_entity_id` 不动。通过。

### 5.4 禁区

- 三份需求 HTML：两树 diff 无名。
- 其他文件：`git diff --name-only` 仅本脚本。
- 幂等键：仍 `seed-bar-${SUFFIX}98`（1 处）。
- 请求体：before/after 抽出的 `-d "{…}"` 体 SHA `7e276ce3d00efdaf4cab557a489d23a507302a4cf5a6f686c2ca5d2a2d5308da`，EQ。
- 预检 / 新功能 / 未发布规则描述：0。
- GR-03：未混入。
- 可执行位：`100755`→`100755`。
- `bash -n`：工作区 exit 0；before blob 语法本就过（本席不把 /tmp 副本当证据）。

### 5.5 类型 4 当场复跑（verification-before-completion）

修前（冻结 red 日志 + 本席独立表达式；未改 before 树）：
```
/home/james/audit-tmp/m025_b3r1_red.log
EXISTS=[]  RED: 已存在的「酒吧A98」查不到  RED: 注释仍称「接口必填」  文件版本行: 无  problems=2
```
本席独立：蛇形样例喂 before 键 `m.get('merchantName')` → `''`；before 含 `只为接口必填`。与 red 日志同因。

修后本席当场：
```
python3 /home/james/audit-tmp/m025_selfcheck.py
→ EXISTS=[mch_1]  文件版本行: ['# 文件版本：V0.01']  problems=0  PY_EXIT=0
```
stdout SHA256 `19e0a090bfdb99f1ab2fa19b4a3d16876ad231c8f4e7cd5ccd234a70322517a7` 与 `/home/james/audit-tmp/m025_b3r1_green.log` 相同。

独立绿表达式：同一蛇形样例喂 `m.get('merchant_name')` → `'mch_1'`。

```
bash -n ops/160/seed-dev-bars.sh → BASH_N=0
```

执行日志 `/home/james/audit-tmp/m025_b3r1_run.log`：BEFORE/AFTER tree、diff sha、1 file 3+/2-、hunks=3、after 内容 SHA `b44ad5d261…`、`BASH_N_EXIT=0`、`mode=775` / `index-mode-after-add=100755` 与本席实算一致。本席不把该日志当唯一证据，只作交叉。自检脚本只读抽出表达式、喂样例，不调接口。本席未运行播种脚本。

**内容票：PASS。**

---

## 6. ponytail-review（只读）

无额外可删抽象。版本行是 001 指定插入，不是膨胀。注释只改前半。查重仍是原表达式换键。  
`net: -0 lines possible.`  
Lean already. Ship.

---

## 7. 最终票

| 轴 | 票 | 依据 |
|---|---|---|
| 归因（闸 3） | PASS | 3+/2- 均可归 M025-001、M025-002；版本行归 001；无无 ID 行 |
| 内容 | PASS | 两条最小处置落地；与 SNAKE_CASE / Tests:150 / `businessEntityId` 可空一致；POST 体与幂等键未改；无预检/未发布规则/他文件；`100755` 保留；`bash -n` 0；自检红→绿 |
| 本席 B4-r1 | PASS | 两轴均 PASS；任一 FAIL 即 FAIL，此处未触发 |

本票不是模块 PASS，不是授权改归属。B4 双 PASS 仍须席甲本次完整 raw 合取。本席未读 self-raw。

步骤：B4-r1。模块 M025。B3 次数＝本轮审的 r1。未写 findings/ledger。
