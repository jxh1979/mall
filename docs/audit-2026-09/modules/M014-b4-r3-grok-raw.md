CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M014 B4-r3 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r3
席位：固定 Grok 只读独立审计席乙（grok-4.6/xhigh）
计划：160-audit-prompt.md V0.17（本席读 B4 / §5；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M014-b4-r3-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r3 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B2 双 PASS 票替代本次实际 diff 审计。席甲自审票不得覆盖本席；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。B3 累计已记 3（历史 1+r2+本次 r3，已达 3 次上限）；本席未因获批或额度降低任何标准。第 3 次 B3 后仍须完成本 B4；本席 PASS 不预支 B5。

未读取：任何 `M014-b4-r3-self-raw.md` 正文（`ls -l` 见该路径存在、16450 bytes、mtime 2026-09-19 16:18 PDT；本席未 `read`/`cat`/`head`/`grep` 其内容）。ledger 顶部席甲封存摘要不是本席证据，本票只依两树 diff、定稿 7 处拟改、HTML/代码只读对照。可读：V0.17 B4/§5、findings.md V2.43 顶端定稿、旧 B1/B2/B4/B5。正本 `/home/james/complex-management` 分支 `main`。

本批队列（findings V2.43 定稿，闸 3 唯一来源）：M014-024（细化）＝是；M014-001–023＝否。当前 24 项：1 是、23 否。B3 本批已完成，累计 3。定稿候选段「本批逐句原句与拟改」共 7 处即 B3 执行文本。文档类：可复现命令+输出，不造执行测试；只读源码/HTML 为实现对照，未跑 Java/HTTP/数据库。不增判据、不 LOCK-AMEND、不造新规则/日志/缺口闭合。本文件无变更日志段，不新造。

---

## 1. 输入、独立锁实算、tree 与 diff 指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
cd /home/james/complex-management
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。文件 889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值（对照，非本席抄写来源）。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值（对照）。`LOCK-AMEND-COUNT: 1`。criteria-lock 版本仍 V0.11。两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（本 diff 未改锁）。工作区 lock 字节与 after 树全等。

| 项 | 值 |
|---|---|
| 模块 | M014 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段（`plan/modules.md` 1 文件 OVERSIZE-SINGLE-FILE；标记行数 3494） |
| 阶段 | B4-r3 只读审计 B3 第 3 次整批修复的「修复内容」本身（操作员「从 M009 重新独立审计」续作重开；标准不降；B3 已达 3 次上限） |
| 正式队列「是」（1） | M014-024 |
| 正式队列「否」 | M014-001–023 |
| B3 | 已执行 3 次（历史 1+r2+本次），模块仍 IN_PROGRESS；本席未降标准 |
| before（对象类型 tree） | `f9cdd0f0f043cb13b9ff2940085b8bc29b60981d` |
| after（对象类型 tree） | `79438e8ae8cbdcc8b83b49ca43542cde343fd893` |
| `git cat-file -t` / `-s` | 两者均为 `tree`；size 各 987（raw tree 对象）。pretty-print 各 1673 bytes、26 行 / 26 TAB |
| pretty-print SHA256 | before `e734e66f785bce980b77b2a2eb4790a8418bb4a5b954ae7e7c82ad4f5e2c2d4d`；after `74f99d42b41ee0fc88c4d567d0f3994c81585e4ba41144029f329018ea212437` |
| 完整 diff 命令 | `git diff --binary f9cdd0f0f043cb13b9ff2940085b8bc29b60981d 79438e8ae8cbdcc8b83b49ca43542cde343fd893`（无路径过滤） |
| 实际差异 | 8614 bytes；SHA256 `062f7c6f60566312e499c78600e39b2a0739fc6f0f99c11249d9a338a3ed56a4`（sha256sum 与 python hashlib 同值）；1 文件；7 hunk；+8 / −4；62 个 LF、0 个 CR、0 个 NUL |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3790 / after 3790；路径 `comm -3` 空（无增删路径）；递归 blob 比较仅 1 路径哈希变化：`docs/platform-governance-product-spec-v0.2.md` |
| 顶层 tree 对象 | 仅 `docs` 条目哈希变化：`68567c7f7351c92acf1d3f9c2697afb057cedbd3` → `4604adca5ef79ec33fa97e6bbc5bdd8236ee5b03`；`.claude` / `apps` / `backend` / `index` / 其它顶层条目 before=after |
| HEAD（对照，非本 diff 基线） | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}`=`ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。本席只审派发 before/after，不以 HEAD 或事后提交替换 |
| 真实索引 | `git write-tree` = `a86628fd3f8b13c88aa625589041296f37aec19e` ≠ after、≠ HEAD^{tree}。规格文件真实索引 blob 仍 `ed8b55d19314c59be042ebb511710fbdb0185af4`（早于本批 before `70150ed942b9b57c3ac205e7e291ee1be3321589`），≠ after `cdb8cfda6ac8feb8b35fa3f8d6d7e8fc094400e3`。本轮未把修复写入真实索引，与 B3 临时索引口径一致。仓库根文件 `index` 两树 blob 同为 `65d675154f23ffb2d0196e017d44a5e7017550f5`，工作区字节与 after 全等 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，before=after；工作区字节亦与 after 全等 |
| 锁/sidecar 两树 blob | 相同：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| findings/ledger 两树 blob | 相同（本 diff 未改）：findings `14140e35fe075bfa140a7965612974462744ed1c`，ledger `e4278b1969105532b65cd7ea9f5e026cb0c6c40e`。after 树 findings 仍 V2.43、024 修复队列「是」、001–023 否；队列未在本 diff 提前清。工作区 findings 字节与 after 树全等。工作区 ledger 在 after 树之后另有 B3 完成/席甲封存段（本 diff 外，不审；摘要不采信） |
| 源码/迁移/OpenAPI | `git diff --name-only` 仅规格一 md；AccessService / AccessController / PlatformService / PlatformController / V427 / `docs/openapi/openapi-p0.yaml` before=after |
| 其他文档 | 集团 PRD / 验收包 / 平台治理 PRD / 绑定设计 / merchant-space-contract-model before=after |
| 测试面 | 文档类；本席只读对照，未执行 Maven/Java/HTTP/DB |
| 合并状态 | 无 MERGE_HEAD / rebase-merge / rebase-apply / CHERRY_PICK_HEAD。`On branch main` |
| questions/M014* | 无 |
| 添加项 / HTML 申请 | 0 / 0 |
| 当地日 | `date`：2026-09-19 16:17 PDT；`date -u`：2026-09-19 23:17 UTC。变更记录日期 2026-09-19 与当地日、UTC 日一致 |

`git diff --stat --binary`：

```
 docs/platform-governance-product-spec-v0.2.md | 12 ++++++++----
 1 file changed, 8 insertions(+), 4 deletions(-)
```

`git diff --numstat`：`8	4	docs/platform-governance-product-spec-v0.2.md`

`git diff --name-status`：1 处 `M`，无 A/D/R。

after 树规格 vs 工作区（`git cat-file -p <after>:<path>` SHA256 + 行数；工作区 `open().read()`）：

| 文件 | 行 | SHA256 | bytes | before=after | 工作区=after |
|---|---|---|---|---|---|
| docs/platform-governance-product-spec-v0.2.md | 3500 | `f5557682f6a6872c2464630ce493101e45c3f014b0fc9e2d593ae5b83c1ec92f` | 229864 | 否 | 是 |

before 规格：3496 行 / 228454 bytes / SHA256 `f74c793e2db234b9ec458962c70e71112e1887d2285f2600c12be62bc1c45e48`。净 +4 与 +8/−4 一致。

git blob：

| 文件 | before blob | after blob |
|---|---|---|
| platform-governance-product-spec-v0.2.md | `70150ed942b9b57c3ac205e7e291ee1be3321589` | `cdb8cfda6ac8feb8b35fa3f8d6d7e8fc094400e3` |
| group-governance-rebuild-prd-v1.0.md | `d252bdc30d89bc195f80cb8ed6eda835df60de84` | 同左 |
| platform-governance-acceptance-pack-v0.2.md | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` | 同左 |
| platform-governance-prd-v0.1.md | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` | 同左 |
| business-entity-binding-development-design-v0.1.md | `583106e516a7519e3eccfaf4789a56428a38ec63` | 同左 |
| merchant-space-contract-model-v0.1.md | `b3b36f045d7ebef9907d195c0df540ded1efbe98` | 同左 |

代码/OpenAPI 两树 blob（未动）：

| 路径 | blob（before=after） |
|---|---|
| AccessService.java | `3720af8cf3a6a5d1b114789b8342fda2b790be1d` |
| AccessController.java | `5524ed405227e0778aebc89257ed9995eff2b9d9`（`PUT /users/{id}/staff-level` 仍在 :262） |
| PlatformService.java | `b325adc09bd66d73505130138889b7a87f9bd211` |
| PlatformController.java | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |
| V427__m009_project_profiles_and_associations.sql | `e485a5d29fea55fa714091485aa26facc8c45160` |
| docs/openapi/openapi-p0.yaml | `1f056b360dc60c37b6c026c761f0fa57b3688e63` |

`plan/modules.md` `^## M[0-9]` 计数 471。after 树 findings 版本 V2.43。本轮只用已冻结判据核「拟改是否落地 / 是否越界 / 是否与 HTML·同文废止行一致」，不 LOCK-AMEND。四分类不重开。冻结 ID 存在性只核：`GEN-001` / `GEN-008` / `GEN-016` / `PGM-008` / `BSM-028` / `BEQR-157` / `BEQR-159` 均在 `criteria-lock.md`（V0.11），本席不增判据。

---

## 2. 强制技能预检与按意图执行（铁律 3）

环境实核（列目录 / 读 SKILL.md），缺则记缺口，不冒充已调用。

| 技能 | 存在 | 本轮用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| using-arc | 是 | 路由：用户指令优先于 Arc 全量审计/评审工作流。Arc audit/review 本体要派子代理、写 `docs/arc/audits/`、向人提问 → **不执行本体** |
| graft | 是（SKILL.md 存在） | **未运行** `graft` 命令：工具会刷新 `graft/` 图，超出本席唯一可写路径。规格/HTML/AccessService 以 after 树 `git cat-file` 为准 |
| grilling / grill-me | 是（`skills/productivity/grilling`） | 对抗式自问结构；**不等操作员、不派子代理**。grilling 本体要问人 → 不执行本体。事实自己读 |
| batch-grill-me | **缺口** | 技能目录无此名 |
| ubiquitous-language | **缺口** | 技能目录无此名 |
| requesting-code-review | 是 | B4＝本席即审；**不派** reviewer 子代理、不用 `HEAD~1`、不即刻修。基线=派发 tree id |
| receiving-code-review | 是 | 本席是外审不是收票方；只借「先核实再下票、禁止表演性同意」；**不 IMPLEMENT** |
| verification-before-completion | 是 | PASS 前实跑 sha256/wc/`git diff --binary`/`git cat-file`/`git diff --check`/7 处拟改/HTML·同文废止行对照 |
| systematic-debugging | 是 | 只借读错误→追数据流；**不加** diagnostic instrumentation。B4 期间不修 |
| diagnosing-bugs | 是 | 只读追拟改落地与表单元完整；文档类无红循环命令，不造 harness |
| test-driven-development / tdd | 是 | 文档类**不强制**落测试；未造假测试 |
| codebase-design | 是 | 删除测试：本 diff 无新模块接口、无加深/抽缝 |
| ponytail / ponytail-review | 是 | B3 已完成；本席审多余行。净 +8/−4，无旁路抽象 |
| i-have-adhd | 是 | 编号、复述「模块 M014 / 共 471，B3 第 3 次」；本 raw 承载正文。CLI 按派发不回正文 |
| writing-for-agents | 是 | 本 raw 给席甲/操作员读：完成条件可检查（tree/SHA/7 处/逐行归属） |

---

## 3. 定稿 7 处拟改是否逐字落地

闸 3 唯一来源：findings V2.43「本批逐句原句与拟改」。after 树逐条对（python `==` 全等，非摘要）：

| # | ID | 拟改要点 | after 行 | 落地 |
|---|---|---|---|---|
| 1 | 024 版本 | `# 平台治理产品规格 V0.08` | 1 | 是。before 行 1 为 V0.07。版本 +0.01 |
| 2 | 024 说明 | 文首加 2026-09-19 一行（B3-r3，V0.07→V0.08，024 一句）+ 空行，R5 行不动 | 10–12 | 是。行 10 与拟改说明句逐字相等；行 11 空；行 12 仍为 R5 标注原句。行 8 B3-r2 说明未改 |
| 3 | 024 | `\| 默认职级 \| … \| 应选择已有职级。【已废止/不得回灌】「应选择启用职级」 \| 变更需留痕 \|` | 1060 | 是。before :1058 原句唯一。6 列 / 7 个 `\|`，与邻行岗位字段表同构 |
| 4 | 024 | `\| 当班岗位 \| … \| 应为已有岗位。【已废止/不得回灌】「应为启用岗位」 \| 变更需留痕 \|` | 1074 | 是。before :1072 原句唯一。6 列完整 |
| 5 | 024 | `\| 【已废止/不得回灌】岗位当前不可分配员工 \| 不采用——岗位状态本规格不定义，三份业务 HTML 无对应状态枚举；停岗/离职按员工状态处理 \|` | 1854 | 是。before :1851 原句唯一。2 列 / 3 个 `\|`，与 F7 异常表同构 |
| 6 | 024 字段字典§19 | 2026-09-19 一行插在 2026-09-11 之前 | 1321 | 是。与拟改 6 逐字相等。历史行未改。页面清单§20 / 低保真§23 **未**加 024 行 |
| 7 | 024 流程图§17 | 2026-09-19 一行插在既有 2026-09-19（021）之前 | 2237 | 是。与拟改 7 逐字相等。021 历史行仍在 :2238 |

未新造规则：024 只把三处现行启用/可分配门控改成与 :1042/:1063/:3375 同根的废止写法，未新写岗位状态字段、未发明启用闭集。未新造日志段（字段字典§19 与流程图§17 既有变更日志各加一行；页面清单§20、低保真§23 本批不加）。全文无「缺口闭合」。

---

## 4. 逐变更行归因（闸 3；无无 ID 行）

版本行 / 说明行 / 变更记录行归本批 ID。下列为 diff 中全部 `+`/`-` 行（不含 `+++`/`---`），共 8+4=12 行。

| hunk | 符号 | 内容摘要 | 归属 |
|---|---|---|---|
| 1 @@ -1,4 +1,4 | − | `# 平台治理产品规格 V0.07` | 024 版本 |
| 1 | ＋ | `# 平台治理产品规格 V0.08` | 024 版本 |
| 2 @@ -7,6 +7,8 | ＋ | `> 2026-09-19 160 全量审计 M014（B3-r3，…（024）。` | 024 说明 |
| 2 | ＋ | 空行（拟改 2 在新说明与 R5 行之间） | 024 说明 |
| 3 @@ -1055,7 +1057,7 | − | `应选择启用职级` | 024 |
| 3 | ＋ | `应选择已有职级。【已废止/不得回灌】「应选择启用职级」` | 024 |
| 4 @@ -1069,7 +1071,7 | − | `应为启用岗位` | 024 |
| 4 | ＋ | `应为已有岗位。【已废止/不得回灌】「应为启用岗位」` | 024 |
| 5 @@ -1316,6 +1318,7 | ＋ | 字段字典§19 `2026-09-19` 行 | 024 |
| 6 @@ -1848,7 +1851,7 | − | `岗位当前不可分配员工 \| 不允许继续分配` | 024 |
| 6 | ＋ | 整行标【已废止/不得回灌】并注岗位状态本规格不定义 | 024 |
| 7 @@ -2231,6 +2234,7 | ＋ | 流程图§17 `2026-09-19` F7 行 | 024 |

无第二 ID、无「顺手」空白/格式/历史日志改写、无测试/源码/HTML 行。归因 PASS。

---

## 5. 派发核项（命令证据）

### 5.1 024 与同文废止写法、ENUM、治理 HTML

对照行（before 行号 → after；字节全等，仅插入下移）：

| 锚点 | before | after | 文本要点 | 本 diff |
|---|---|---|---|---|
| §9.1 岗位 | 1040 | 1042 | `选择已有岗位。【已废止/不得回灌】「必须选择启用岗位」` | 未改 |
| 默认权限角色 | 1060 | 1062 | `应选择启用角色` | **未改**（hunk 3 上下文行仍在） |
| §9.2 是否可分配员工 | 1061 | 1063 | `【已废止/不得回灌】是否可分配员工` + 岗位状态本规格不定义 | 未改 |
| F7 员工停岗或离职 | 1850 | 1853 | `不允许加入新班次` | 未改（024 注「停岗/离职按员工状态处理」指向此行） |
| 补录 | 3129 | 3133 | `\| 岗位停用 \| 不允许继续分配 \|` | **未改** |
| 岗位状态三态 | 3371 | 3375 | `【已废止/不得回灌】岗位状态三态` + 员工状态五值 | 未改 |

after :1060 / :1074 采用与 :1042 相同的「已有 X + 【已废止/不得回灌】引用旧启用句」；after :1854 采用与 :1063/:3375 相同的整行【已废止/不得回灌】并写明岗位状态本规格不定义。与拟改及同根写法一致。

ENUM（BEQR 工作区 = after 树，未改）：

- :240 `员工状态`：待入职、在职、试岗、停岗、离职（五值）。:1854 停岗/离职按员工状态处理，不另造岗位可分配状态。
- :232 `职级`：M1–M10，每级分上/中/下。无启用闭集。
- :242 `权限状态`：草稿、已启用、暂停、已停用、归档。故 :1062「应选择启用角色」保留，不随 024 改。

治理 HTML PGM :363（工作区 = after 树）：`管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态。` 卡片只提「岗位状态」概念，无取值枚举。:1854 注「三份业务 HTML 无对应状态枚举」与此一致。未改 HTML。

### 5.2 021–023 修复未回退

本 diff 7 hunk 均不落在 021/022/023 原修复行。after 仍有：

- :1985 F9 末句 `本规则不把任意手工模板应用列为触发条件。`（无「仅职级变化、责任范围变化」）
- :1996 `职级不替代功能权限点；敏感权限授出仍按五类高危先审批`
- :236 / :2423 `\| 业态列表 \| 业态名称、说明 \|`；`业态名称、编号、说明` 命中 0
- :768 / :2725 后续细化 11/12 止于「承接落地。」+ V394 句；正文无「所有对象继续携带 `project_id` 并按项目归属聚合」（仅 B3-r2 说明行与 §20/§23 历史变更记录描述删除）
- 要求句仍在 :752 / :2708（「保证所有对象带综合体项目归属」）
- 页面清单§20 :784 与低保真§23 :2737 仍为 r2 的 022/023 变更记录，本批未再加行
- 流程图§17 :2238 仍为 r2 的 021 变更记录

### 5.3 未动面

三份业务 HTML、锁、sidecar、主索引、HEAD、backend 树、OpenAPI、集团 PRD、验收包、平台治理 PRD、绑定设计、merchant-space-contract-model、findings 队列：两树 blob 或路径清单已证 before=after。工作区三 HTML / AccessService / OpenAPI / 根 `index` / lock = after 树。未改 AccessService（1305–1313 改职级重算注释仍在，只读对照，非本批修复面）。

---

## 6. 对抗式（grilling 结构，不问人）

1. 若把 024 写成新「已有职级必须存在」业务规则而不是废止门控，会越界。实物是与 :1042 同根的废止引用，未新写岗位状态字段。不 FAIL。
2. 「第1058/1072/1851行」在 after 因文首 +2、字段字典日志 +1 下移到 :1060/:1074/:1854。拟改字符串逐字在 after，原句在 before 各 1 次。不 FAIL。
3. :1062「应选择启用角色」若被顺手改掉会与 ENUM :242 权限状态「已启用」冲突。hunk 3 上下文显示该行未改。不 FAIL。
4. :3133 补录「岗位停用」仍无废止标记。定稿写明不改。不 FAIL。
5. 页面清单§20 / 低保真§23 若加 024 行会超出拟改 7 处。after 该两表首条数据仍是 r2 的 022/023 行。不 FAIL。
6. 说明行/变更日志仍出现「启用职级」「启用岗位」是在描述删除并标废止，不是回灌现行校验。活句 :1060/:1074/:1854 已废止。不 FAIL。
7. 真实索引仍旧 blob。符合 B3 临时索引。不 FAIL。
8. findings 仍一是，未提前清队。符合 B4 前口径。不 FAIL。
9. 无无 ID 行、无第二文件、无源码/HTML/锁。不 FAIL。
10. 未造执行测试、未冒称 Java/HTTP/DB 实跑。不 FAIL。
11. 工作区 ledger 比 after 树多出席甲封存段。不在本 diff 内，不审、不采信。不 FAIL。
12. B3 已达 3 次上限不构成内容缺陷；本席未降标准。不 FAIL。

无可归本批「是」ID 的残留缺陷，亦无无 ID 越界。不回 B2/B3。

---

## 7. 结论

修复内容：PASS。7 处拟改逐字落地；表单元完整；版本 V0.07→V0.08；变更记录日期 2026-09-19；不新造规则/日志/缺口闭合；024 与同文 :1042/:1063/:3375 废止写法、ENUM :240 员工状态五值、:242 权限状态、PGM :363 一致；:1062「应选择启用角色」与 :3133 补录未动；021–023 修复未回退。

范围归因：PASS。7 hunk、+8/−4 全部归 024（版本/说明/两张变更记录归本批 ID）；无无 ID 行；三 HTML/锁/主索引/代码/OpenAPI/其他文档未动。

总结论：PASS。本席不行使否决。已冻结判据不增判据。

---

## 8. 两个 tree 对象原文（`git cat-file -p`，TAB 分隔 object 与路径）

before `f9cdd0f0f043cb13b9ff2940085b8bc29b60981d`（pretty-print 1673 bytes，SHA256 `e734e66f785bce980b77b2a2eb4790a8418bb4a5b954ae7e7c82ad4f5e2c2d4d`，26 行 / 26 TAB）：

```
040000 tree 08947cd2a6819915194f6eeb00dc393153ec284a	.claude
100644 blob 73ec4e6be98e3aef0818c9980db4bb1c0c3b9e21	.env.example
040000 tree 225f228fff5e6761fe7d9d2d9dec28f0491a0c8e	.github
100644 blob 5534d829c1db85bb6e57fd8fc0079912f38e70d9	.gitignore
040000 tree 83a3c7afa5a500e1981e8e1da99a9b48b2edbc51	.grok
100644 blob 615e581e9799f0b9c7d8047439211fc5f19d3329	.ignore
100644 blob fcedf6ac1da42226414f23167f54e0d62faa0f9d	.mcp.json
100644 blob 76b7a11c30b508648c9ea22153663d6117fa13ad	.semgrepignore
100644 blob d2a264b09936f1522e34480b6940375cf968e7cc	AGENTS.md
100644 blob 45534fc58d54d285e7da057e79f1fb11a8c05924	CONTEXT.md
100644 blob 6bf56366fbe8eef65bcc31d59c9da674017af807	PROGRESS.md
100644 blob 271e4566af87a6e0b31fec4b451e15ac3435cd44	README.en.md
100644 blob a73d207809a20b8addc79157e0dacfa2e61f76c5	README.md
040000 tree eb8d7be2f34741196d5e6546afa02e56904f6d4f	apps
040000 tree 1ebe13e6d9ed813f92724d361fd46cea5432fe95	backend
040000 tree 68567c7f7351c92acf1d3f9c2697afb057cedbd3	docs
100644 blob 65d675154f23ffb2d0196e017d44a5e7017550f5	index
040000 tree ad0fa09586cc36a7b0bb0d8dbaee00ba24251907	ops
100644 blob 63ecc802df8de76786dc69101c01abba6bb33d31	package-lock.json
100644 blob 0281eaa9c5d0dd85e055a8c46ae2a66c42028e2d	package.json
100644 blob 0feddca25bb80aef362c0720529c79fe2b0562f7	playwright.config.ts
100644 blob 63f7eb0684fa3620972f13e8f5665b0fd80c3b92	playwright.real-backend.config.ts
040000 tree a9d847ccb9f5cad651d197eea39ebf9ef7d5e225	security-scans
040000 tree 91e5869ed18579dcb04abdce018fe46b4adb82ad	tests
040000 tree cc4d6b27a3abf2b48b94414f3df9a9cd3325b673	tools
100644 blob 60709a9cae77451b7b13c53dd53996a93e7a53e3	tsconfig.base.json
```

after `79438e8ae8cbdcc8b83b49ca43542cde343fd893`（pretty-print 1673 bytes，SHA256 `74f99d42b41ee0fc88c4d567d0f3994c81585e4ba41144029f329018ea212437`，26 行 / 26 TAB）：与上逐行相同，唯一差异为 `docs` 树 `68567c7f7351c92acf1d3f9c2697afb057cedbd3` → `4604adca5ef79ec33fa97e6bbc5bdd8236ee5b03`。`backend` 树仍 `1ebe13e6d9ed813f92724d361fd46cea5432fe95`；根 `index` 仍 `65d675154f23ffb2d0196e017d44a5e7017550f5`。

after 全文：

```
040000 tree 08947cd2a6819915194f6eeb00dc393153ec284a	.claude
100644 blob 73ec4e6be98e3aef0818c9980db4bb1c0c3b9e21	.env.example
040000 tree 225f228fff5e6761fe7d9d2d9dec28f0491a0c8e	.github
100644 blob 5534d829c1db85bb6e57fd8fc0079912f38e70d9	.gitignore
040000 tree 83a3c7afa5a500e1981e8e1da99a9b48b2edbc51	.grok
100644 blob 615e581e9799f0b9c7d8047439211fc5f19d3329	.ignore
100644 blob fcedf6ac1da42226414f23167f54e0d62faa0f9d	.mcp.json
100644 blob 76b7a11c30b508648c9ea22153663d6117fa13ad	.semgrepignore
100644 blob d2a264b09936f1522e34480b6940375cf968e7cc	AGENTS.md
100644 blob 45534fc58d54d285e7da057e79f1fb11a8c05924	CONTEXT.md
100644 blob 6bf56366fbe8eef65bcc31d59c9da674017af807	PROGRESS.md
100644 blob 271e4566af87a6e0b31fec4b451e15ac3435cd44	README.en.md
100644 blob a73d207809a20b8addc79157e0dacfa2e61f76c5	README.md
040000 tree eb8d7be2f34741196d5e6546afa02e56904f6d4f	apps
040000 tree 1ebe13e6d9ed813f92724d361fd46cea5432fe95	backend
040000 tree 4604adca5ef79ec33fa97e6bbc5bdd8236ee5b03	docs
100644 blob 65d675154f23ffb2d0196e017d44a5e7017550f5	index
040000 tree ad0fa09586cc36a7b0bb0d8dbaee00ba24251907	ops
100644 blob 63ecc802df8de76786dc69101c01abba6bb33d31	package-lock.json
100644 blob 0281eaa9c5d0dd85e055a8c46ae2a66c42028e2d	package.json
100644 blob 0feddca25bb80aef362c0720529c79fe2b0562f7	playwright.config.ts
100644 blob 63f7eb0684fa3620972f13e8f5665b0fd80c3b92	playwright.real-backend.config.ts
040000 tree a9d847ccb9f5cad651d197eea39ebf9ef7d5e225	security-scans
040000 tree 91e5869ed18579dcb04abdce018fe46b4adb82ad	tests
040000 tree cc4d6b27a3abf2b48b94414f3df9a9cd3325b673	tools
100644 blob 60709a9cae77451b7b13c53dd53996a93e7a53e3	tsconfig.base.json
```

完整原样 `git diff --binary` 见文末 §9（8614 bytes，SHA256 已核）。

## 9. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary f9cdd0f0f043cb13b9ff2940085b8bc29b60981d 79438e8ae8cbdcc8b83b49ca43542cde343fd893`

before tree = `f9cdd0f0f043cb13b9ff2940085b8bc29b60981d`

after tree = `79438e8ae8cbdcc8b83b49ca43542cde343fd893`

字节 8614；SHA256 `062f7c6f60566312e499c78600e39b2a0739fc6f0f99c11249d9a338a3ed56a4`；UTF-8 完整原文如下。

````diff
diff --git a/docs/platform-governance-product-spec-v0.2.md b/docs/platform-governance-product-spec-v0.2.md
index 70150ed9..cdb8cfda 100644
--- a/docs/platform-governance-product-spec-v0.2.md
+++ b/docs/platform-governance-product-spec-v0.2.md
@@ -1,4 +1,4 @@
-# 平台治理产品规格 V0.07
+# 平台治理产品规格 V0.08
 > 2026-07-04 逻辑一致性修订：§9.6 准入落库口径收口为 supervisor_review + supervisor_review_decisions（admission_audits/contract_admission_rules 从未建）；§14A 终端开关职责边界脚注；补名词四层脚注
 
 > 2026-09-01 M01 全量复审：按三份现行 HTML 补齐金额精度、合同本地 AI、合同当地时间和外部经销商治理的跨篇规格；删除已判称谓切片、复制模板现行支路，并统一集团视图已承接时态。
@@ -7,6 +7,8 @@
 
 > 2026-09-19 160 全量审计 M014（B3-r2，V0.06→V0.07）：F9 强制补充规则去掉与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的仅职级变化、责任范围变化排除，异常分支不再以职级挡位替代功能权限点（021）；页面清单 §18 与低保真 §21 删去「所有对象继续携带 project_id 并按项目归属聚合」假完成声明（022）；业态列表去掉已废止的「编号」列（023）。
 
+> 2026-09-19 160 全量审计 M014（B3-r3，V0.07→V0.08）：岗位字段「默认职级」、班次字段「当班岗位」与 F7 异常分支去掉回灌已废止岗位状态门控的「启用职级」「启用岗位」「岗位当前不可分配员工」校验，按同文 §9.1 岗位行写法改为选择已有职级/岗位并标废止（024）。
+
 > **⚠️ R5 业务HTML缺口审计修正标注（2026-06-18，只读审计后回填，未改代码）**
 > 以业务说明地图为基准核验真实迁移(V001–V065)+代码，本规格涉及的两条治理链与文档声称不符：
 > - **合同准入卡控+分级放行（HTML #contract/#flow 核心链）**：`SupervisorReviewService.evaluate` 是孤立 HTTP 评估接口，票务/酒吧/游乐/结算等业务域**在动作前零调用**它，"超范围→卡住→升级→放行"闸口**不触发**；`AdmissionAuditsPage` 仅聚合 merchants/bindings/contracts 三列表做伪闭环。`contract_admission_rules`/`admission_audits` 两表**从未建**，审批层级实际落 `supervisor_review_decisions`(V057)——本规格 §9.6"合同准入规则页"与"admission_audits.approval_level"表述需收口。
@@ -1055,7 +1057,7 @@ P0 必填字段必须满足至少一个条件：
 | 岗位名称 | 识别岗位 | 必填 | 文本 | 同一主体内不应重复 | 变更需留痕 |
 | 岗位类型 | 区分运营、销售人员、财务、现场、客服、巡检等 | 必填 | 选择 | 应与角色权限相匹配；销售人员岗位用于卡座销售归属和提成倍率判断，不替代权限 | 变更需留痕 |
 | 岗位说明 | 描述岗位职责 | 选填/P0 推荐 | 多行文本 | 应说明主要责任 | 可留痕 |
-| 默认职级 | 岗位对应的默认管理等级 | 选填/P0 推荐 | 关联选择 | 应选择启用职级 | 变更需留痕 |
+| 默认职级 | 岗位对应的默认管理等级 | 选填/P0 推荐 | 关联选择 | 应选择已有职级。【已废止/不得回灌】「应选择启用职级」 | 变更需留痕 |
 | 默认责任区域 | 岗位默认工作空间 | 选填 | 关联选择 | 可为空 | 变更需留痕 |
 | 默认权限角色 | 岗位默认拥有的角色 | 选填/P0 推荐 | 关联选择 | 应选择启用角色 | 变更需留痕 |
 | 【已废止/不得回灌】是否可分配员工 | 不得作为必填开关或通用岗位状态门控。岗位状态本规格不定义——三份业务 HTML 无对应状态枚举 | 不采用 | — | 三 HTML 未批该必填字段 | — |
@@ -1069,7 +1071,7 @@ P0 必填字段必须满足至少一个条件：
 | 开始时间 | 明确上岗开始 | 必填 | 时间 | 应早于结束时间 | 变更需留痕 |
 | 结束时间 | 明确上岗结束 | 必填 | 时间 | 应晚于开始时间 | 变更需留痕 |
 | 当班员工 | 明确责任人 | 必填 | 关联选择 | 应为可上岗员工 | 变更需留痕 |
-| 当班岗位 | 明确职责 | 必填 | 关联选择 | 应为启用岗位 | 变更需留痕 |
+| 当班岗位 | 明确职责 | 必填 | 关联选择 | 应为已有岗位。【已废止/不得回灌】「应为启用岗位」 | 变更需留痕 |
 | 当班区域 | 明确责任空间 | 选填/P0 推荐 | 关联选择 | 应选择有效空间。【已废止/不得回灌】以是否可经营作为选择门控 | 变更需留痕 |
 | 班次主管 | 当班管理责任人 | 选填/P0 推荐 | 关联选择 | 应为有效员工 | 变更需留痕 |
 | 班次状态 | 【已废止/不得回灌】班次状态机及「控制责任链是否生效」 | 不定义 | — | 本规格不定义——三份业务 HTML 无对应状态枚举 | — |
@@ -1316,6 +1318,7 @@ P0 列表页建议统一提供以下筛选字段。
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-19 | 改 | §9.2 岗位字段 / §9.3 班次字段 | 「默认职级」「当班岗位」校验去掉回灌已废止岗位状态门控的「启用职级」「启用岗位」，按 §9.1 岗位行同根写法改为选择已有职级/岗位并标废止 | 160 全量审计 M014-024；ENUM 员工状态五值、职级 M1–M10 无启用闭集；同文 §9.1「必须选择启用岗位」与 §9.2「是否可分配员工」已废止 |
 | 2026-09-11 | 改 | §3.1 / §5 / §6 / §7 / §9.1 修订说明 / §17 | 第十一端产品码统一 dealer_portal 并标未实现；空间类型去「项目」并注归属根（§5 节标题/空间名称/口径订正/类型选项）；「业态编号」标废止、业态名称括注 media；merchant_credentials 改 V028 合规到期台账；修订说明指纹定义改现行；物理实现括注改字典活校验 | 160 全量审计 M014-001/002/009/010/016/017 |
 | 2026-09-01 | 增 | §0A | 补金额、合同 AI/当地时间和外部经销商字段展示与验收输入 | M01 全量复审 ADV-01/02/04/05 |
 | 2026-08-27 | 改 | 合同审批 / 空间状态 / 商户类型 / 业态 / 设备凭证绑定 | 合同生效改为主线（默认总经理＋逐级自动补全）＋业务/财务/法务平行线全部通过；空间六态与状态机改为不定义；商户类型收口为合作模式六类；删创建业态与换机待审；业态页六值闭集 | P2 M01 第二整轮 ①②③④⑤⑥；PGM 合同审批链、ENUM 合作模式/商户业态 |
@@ -1848,7 +1851,7 @@ flowchart TD
 | 注册审批未通过 | 不允许进入正式上岗流程 |
 | 未完成设备凭证或公司工作手机对应 | 可以建档，但不能领取现场作业设备（"NFC"按 D8 改为设备凭证，2026-06-07 收口） |
 | 员工停岗或离职 | 不允许加入新班次 |
-| 岗位当前不可分配员工 | 不允许继续分配 |
+| 【已废止/不得回灌】岗位当前不可分配员工 | 不采用——岗位状态本规格不定义，三份业务 HTML 无对应状态枚举；停岗/离职按员工状态处理 |
 | 班次缺少责任区域 | 允许保存，但提示责任链不完整 |
 | 班次缺责任区域 | 允许保存，但提示责任链不完整 |
 
@@ -2231,6 +2234,7 @@ flowchart TD
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-19 | 改 | §9.6 F7 异常分支 | 「岗位当前不可分配员工 → 不允许继续分配」整行标已废止/不得回灌（岗位状态本规格不定义，三份业务 HTML 无对应状态枚举；停岗/离职按员工状态处理） | 160 全量审计 M014-024；ENUM 员工状态五值；字段字典 §9.2 已废止「是否可分配员工」 |
 | 2026-09-19 | 改 | §11.4 F9 强制补充规则 / §11.5 异常分支 | 删去与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的仅职级变化、责任范围变化排除，只保留任意手工模板应用不触发；职级与敏感权限不匹配改为不以职级替代功能权限点 | 160 全量审计 M014-021；PGM L658/L669；AccessService 改职级重算现状 |
 | 2026-09-11 | 改 | §2 总览图 / §5 F3 / §8 修订说明 | 总览关系图补 F11；F3 合同状态迁移图删除改成员说明、正常流程图非法行改注释；修订说明指纹定义改现行 | 160 全量审计 M014-007/012/013/016 |
 | 2026-09-01 | 改 | §0A / F9 | 补四类 HTML 现行规则的流程承接；删除已废止复制模板支路 | M01 全量复审 ADV-01/02/04/05/06 |
````
