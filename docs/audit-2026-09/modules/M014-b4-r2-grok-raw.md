CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M014 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 只读独立审计席乙（grok-4.6/xhigh）
计划：160-audit-prompt.md V0.17（本席读 B4 / §5；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M014-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B2 双 PASS 票替代本次实际 diff 审计。席甲自审票不得覆盖本席；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。B3 累计已记 2（历史 1+本次）；本席未因获批或额度降低任何标准。

未读取：任何 `M014-b4-r2-self-raw.md` 正文（`ls` 见该路径存在、6952 bytes、mtime 2026-09-19 15:29；本席未 `read`/`cat`/`grep` 其内容）。ledger 顶部席甲封存摘要不是本席证据，本票只依两树 diff、定稿 11 处拟改、HTML/代码只读对照。可读：V0.17 B4/§5、findings.md V2.40 顶端定稿、旧 B1/B2/B4/B5。正本 `/home/james/complex-management` 分支 `main`。

本批队列（findings V2.40 定稿，闸 3 唯一来源）：M014-021（冲突）、022、023（细化）＝是；M014-001–020＝否。当前 23 项：3 是、20 否。B3 本批已完成，累计 2。定稿候选段「本批逐句原句与拟改」共 11 处即 B3 执行文本。文档类：可复现命令+输出，不造执行测试；只读源码/HTML 为实现对照，未跑 Java/HTTP/数据库。不增判据、不 LOCK-AMEND、不造新规则/日志/缺口闭合。本文件无变更日志段，不新造。

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
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身（操作员「从 M009 重新独立审计」续作重开；标准不降） |
| 正式队列「是」（3） | M014-021、022、023 |
| 正式队列「否」 | M014-001–020 |
| B3 | 已执行 2 次（历史 1+本次），模块仍 IN_PROGRESS；本席未降标准 |
| before（对象类型 tree） | `ad0dc4fc15b422440bb24109f489fea47510d639` |
| after（对象类型 tree） | `9a975ed491a93d3029f30e7cb415e0ec609b1487` |
| `git cat-file -t` / `-s` | 两者均为 `tree`；size 各 987 |
| 完整 diff 命令 | `git diff --binary ad0dc4fc15b422440bb24109f489fea47510d639 9a975ed491a93d3029f30e7cb415e0ec609b1487`（无路径过滤） |
| 实际差异 | 14429 bytes；SHA256 `09c9e3f94125b63bc841b0c7493f9378956d315634033cefdfd50b779019e3d6`；1 文件；10 hunk；+12 / −7；96 个 LF、0 个 CR、0 个 NUL |
| `git diff --check` | exit 0（无空白错误） |
| 两树路径清单 | before 3786 / after 3786；`comm -3` 空（无增删路径）；递归 blob 比较仅 1 路径哈希变化 |
| 顶层 tree 对象 | 仅 `docs` 条目哈希变化：`25dbc00ae26a12f1330153ad4dcc5c6b64cbb11c` → `a5223c1f22903cf97d17cb2ce84da5dce95e836b`；`.claude` / `apps` / `backend` / `index` / 其它顶层条目 before=after |
| HEAD（对照，非本 diff 基线） | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}`=`ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。本席只审派发 before/after，不以 HEAD 或事后提交替换 |
| 真实索引 | `git write-tree` = `a86628fd3f8b13c88aa625589041296f37aec19e` ≠ after、≠ HEAD^{tree}。规格文件真实索引 blob 仍 `ed8b55d19314c59be042ebb511710fbdb0185af4`（= before = HEAD），≠ after `70150ed942b9b57c3ac205e7e291ee1be3321589`。本轮未把修复写入真实索引，与 B3 临时索引口径一致。仓库根文件 `index` 两树 blob 同为 `65d675154f23ffb2d0196e017d44a5e7017550f5`，工作区字节与 after 全等 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，before=after；工作区字节亦与 after 全等 |
| 锁/sidecar 两树 blob | 相同：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| findings/ledger 两树 blob | 相同（本 diff 未改）：findings `83b06b1a6ed00c976573eadbc6846ecbd2033bfe`，ledger `8f0befcb4f8022972313a6326a7faf9e67a78890`。after 树 findings 仍 V2.40、021/022/023 修复队列「是」、001–020 否；队列未在本 diff 提前清。工作区 findings 字节与 after 树全等。工作区 ledger 在 after 树之后另有 B3 完成/席甲封存段（本 diff 外，不审） |
| 源码/迁移/OpenAPI | `git diff --name-only` 仅规格一 md；AccessService / AccessController / PlatformService / PlatformController / V427 / `docs/openapi/openapi-p0.yaml` before=after |
| 其他文档 | 集团 PRD / 验收包 / 平台治理 PRD / 绑定设计 / merchant-space-contract-model before=after（见下表） |
| 测试面 | 文档类；本席只读对照，未执行 Maven/Java/HTTP/DB |
| 合并状态 | 无 MERGE_HEAD / rebase-merge / rebase-apply / CHERRY_PICK_HEAD。`On branch main` |
| questions/M014* | 无 |
| 添加项 / HTML 申请 | 0 / 0 |
| 当地日 | `date`：2026-09-19 15:29 PDT；`date -u`：2026-09-19 22:29 UTC。变更记录日期 2026-09-19 与当地日、UTC 日一致 |

`git diff --stat --binary`：

```
 docs/platform-governance-product-spec-v0.2.md | 19 ++++++++++++-------
 1 file changed, 12 insertions(+), 7 deletions(-)
```

`git diff --numstat`：`12	7	docs/platform-governance-product-spec-v0.2.md`

`git diff --name-status`：1 处 `M`，无 A/D/R。

after 树规格 vs 工作区（`git cat-file -p <after>:<path>` SHA256 + 行数；工作区 `open().read()`）：

| 文件 | 行 | SHA256 | bytes | before=after | 工作区=after |
|---|---|---|---|---|---|
| docs/platform-governance-product-spec-v0.2.md | 3496 | `f74c793e2db234b9ec458962c70e71112e1887d2285f2600c12be62bc1c45e48` | 228454 | 否 | 是 |

git blob：

| 文件 | before blob | after blob |
|---|---|---|
| platform-governance-product-spec-v0.2.md | `ed8b55d19314c59be042ebb511710fbdb0185af4` | `70150ed942b9b57c3ac205e7e291ee1be3321589` |
| group-governance-rebuild-prd-v1.0.md | `d252bdc30d89bc195f80cb8ed6eda835df60de84` | 同左 |
| platform-governance-acceptance-pack-v0.2.md | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` | 同左 |
| platform-governance-prd-v0.1.md | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` | 同左 |
| business-entity-binding-development-design-v0.1.md | `583106e516a7519e3eccfaf4789a56428a38ec63` | 同左 |
| merchant-space-contract-model-v0.1.md | `b3b36f045d7ebef9907d195c0df540ded1efbe98` | 同左 |

代码/OpenAPI 两树 blob（未动）：

| 路径 | blob（before=after） |
|---|---|
| AccessService.java | `3720af8cf3a6a5d1b114789b8342fda2b790be1d` |
| AccessController.java | 两树同（`PUT /users/{id}/staff-level` 仍在 :262） |
| PlatformService.java | `b325adc09bd66d73505130138889b7a87f9bd211` |
| PlatformController.java | `52d3f0d27191d6fd4cc923a1607a9263bc3eb7fa` |
| V427__m009_project_profiles_and_associations.sql | `e485a5d29fea55fa714091485aa26facc8c45160` |
| docs/openapi/openapi-p0.yaml | `1f056b360dc60c37b6c026c761f0fa57b3688e63` |

`plan/modules.md` `^## M[0-9]` 计数 471。after 树 findings 版本 V2.40。本轮只用已冻结判据核「拟改是否落地 / 是否越界 / 是否与 HTML·代码现状一致」，不 LOCK-AMEND。四分类不重开。冻结 ID 存在性只核：`GEN-001` / `GEN-002` / `GEN-016` / `PGM-006` 均在 `criteria-lock.md`（V0.11），本席不增判据。

before 规格 3491 行；after 3496 行；净 +5 与 +12/−7 一致。

---

## 2. 强制技能预检与按意图执行（铁律 3）

环境实核（列目录 / 读 SKILL.md），缺则记缺口，不冒充已调用。

| 技能 | 存在 | 本轮用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| using-arc | 是 | 路由：用户指令优先于 Arc 全量审计/评审工作流。Arc audit/review 本体要派子代理、写 `docs/arc/audits/`、向人提问 → **不执行本体** |
| graft | 是（SKILL.md 存在） | **未运行** `graft` 命令：工具会刷新 `graft/` 图，超出本席唯一可写路径。AccessService / 规格以 after 树 `git cat-file` 为准 |
| grilling / grill-me | 是（`skills/productivity/grilling`） | 对抗式自问结构；**不等操作员、不派子代理**。grilling 本体要问人 → 不执行本体。事实自己读 |
| batch-grill-me | **缺口** | 技能目录无此名 |
| ubiquitous-language | **缺口** | 技能目录无此名 |
| requesting-code-review | 是 | B4＝本席即审；**不派** reviewer 子代理、不用 `HEAD~1`、不即刻修。基线=派发 tree id |
| receiving-code-review | 是 | 本席是外审不是收票方；只借「先核实再下票、禁止表演性同意」；**不 IMPLEMENT** |
| verification-before-completion | 是 | PASS 前实跑 sha256/wc/`git diff --binary`/`git cat-file`/`git diff --check`/11 处拟改/HTML·代码对照 |
| systematic-debugging | 是 | 只借读错误→追数据流；**不加** diagnostic instrumentation。B4 期间不修 |
| diagnosing-bugs | 是 | 只读追拟改落地与删后句子完整；文档类无红循环命令，不造 harness |
| test-driven-development / tdd | 是 | 文档类**不强制**落测试；未造假测试 |
| codebase-design | 是 | 删除测试：本 diff 无新模块接口、无加深/抽缝 |
| ponytail / ponytail-review | 是 | B3 已完成；本席审多余行。净 +12/−7，无旁路抽象 |
| i-have-adhd | 是 | 编号、复述「模块 M014 / 共 471，B3 第 2 次」；本 raw 承载正文。CLI 按派发不回正文 |
| writing-for-agents | 是 | 本 raw 给席甲/操作员读：完成条件可检查（tree/SHA/11 处/逐行归属） |

---

## 3. 定稿 11 处拟改是否逐字落地

闸 3 唯一来源：findings V2.40「本批逐句原句与拟改」。after 树逐条对：

| # | ID | 拟改要点 | after 行 | 落地 |
|---|---|---|---|---|
| 1 | 021/022/023 版本 | `# 平台治理产品规格 V0.07` | 1 | 是。before 行 1 为 V0.06。版本 +0.01 |
| 2 | 021/022/023 说明 | 文首加 2026-09-19 一行（B3-r2，V0.06→V0.07，021/022/023 各一句）+ 空行，R5 行不动 | 8–10 | 是。行 8 与拟改全文逐字相等；行 9 空；行 10 仍为 R5 标注原句 |
| 3 | 021 | `本规则不把任意手工模板应用列为触发条件。` | 1982 | 是。该句不再含「仅职级变化、责任范围变化或」。删后仍为完整句 |
| 4 | 021 | `\| 职级与敏感权限不匹配 \| 职级不替代功能权限点；敏感权限授出仍按五类高危先审批 \|` | 1993 | 是。表行两格完整 |
| 5 | 022 | 删「；所有对象继续携带 `project_id` 并按项目归属聚合」，句止于「承接落地。」 | 766 | 是。V394 句仍接在其后（定稿「V394 句保留」；拟改 5 原句是唯一子串，非整行抹掉） |
| 6 | 022 | 同上，低保真后续细化 12 | 2721 | 是。V394 句保留 |
| 7 | 023 | `\| 业态列表 \| 业态名称、说明 \|`；`业态详情 \| 业态说明、引用关系` 不动 | 234–235 | 是。表单元完整 |
| 8 | 023 | `\| 业态列表 \| 业态名称、说明 \|`；`业态详情 \| 说明、引用关系` 不动 | 2419–2420 | 是 |
| 9 | 022/023 页面清单§20 | 2026-09-19 一行插在 2026-09-11 之前 | 782 | 是。与拟改 9 逐字相等。历史行未改 |
| 10 | 021 流程图§17 | 2026-09-19 一行插在 2026-09-11 之前 | 2234 | 是。与拟改 10 逐字相等 |
| 11 | 022/023 低保真§23 | 2026-09-19 一行插在 2026-09-11 之前 | 2733 | 是。与拟改 11 逐字相等 |

字段字典§19 变更日志：after 行 1315–1319 首条数据仍为 `2026-09-11`，该段无 `2026-09-19`。符合「字段字典本批未改，不加行」。

未新造规则：021 只删排除并改异常格，未把职级写入正向触发清单（定稿最小边界如此）。未新造日志段（三张已有变更日志各加一行，规格无第四张日志）。全文无「缺口闭合」。无「业态代码」字段。M014-009 不重开：after 行 930（before 927，文首 +2 + 页面日志 +1）仍为「【已废止/不得回灌】业态编号」；行 929 业态名称仍为六值闭集。

---

## 4. 逐变更行归因（闸 3；无无 ID 行）

版本行 / 说明行 / 变更记录行归本批 ID。下列为 diff 中全部 `+`/`-` 行（不含 `+++`/`---`），共 12+7=19 行。

| hunk | 符号 | 内容摘要 | 归属 |
|---|---|---|---|
| 1 @@ -1,10 +1,12 | − | `# 平台治理产品规格 V0.06` | 021/022/023 版本 |
| 1 | ＋ | `# 平台治理产品规格 V0.07` | 021/022/023 版本 |
| 1 | ＋ | `> 2026-09-19 160 全量审计 M014（B3-r2，…（023）。` | 021/022/023 说明 |
| 1 | ＋ | 空行（拟改 2 在新说明与 R5 行之间） | 021/022/023 说明 |
| 2 @@ -229,7 +231,7 | − | `\| 业态列表 \| 业态名称、编号、说明 \|` | 023 |
| 2 | ＋ | `\| 业态列表 \| 业态名称、说明 \|` | 023 |
| 3 @@ -761,7 +763,7 | − | 后续细化 11 含「；所有对象继续携带 `project_id`…」 | 022 |
| 3 | ＋ | 后续细化 11 止于「承接落地。」+ V394 句 | 022 |
| 4 @@ -777,6 +779,7 | ＋ | 页面清单§20 `2026-09-19` 行 | 022/023 |
| 5 @@ -1976,7 +1979,7 | − | F9 末句含「仅职级变化、责任范围变化或」 | 021 |
| 5 | ＋ | F9 末句「本规则不把任意手工模板应用列为触发条件。」 | 021 |
| 6 @@ -1987,7 +1990,7 | − | `阻止保存或要求高级审批` | 021 |
| 6 | ＋ | `职级不替代功能权限点；敏感权限授出仍按五类高危先审批` | 021 |
| 7 @@ -2228,6 +2231,7 | ＋ | 流程图§17 `2026-09-19` 行 | 021 |
| 8 @@ -2412,7 +2416,7 | − | 低保真业态列表含「编号」 | 023 |
| 8 | ＋ | 低保真业态列表「业态名称、说明」 | 023 |
| 9 @@ -2714,7 +2718,7 | − | 后续细化 12 含 project_id 假完成 | 022 |
| 9 | ＋ | 后续细化 12 止于「承接落地。」+ V394 句 | 022 |
| 10 @@ -2726,6 +2730,7 | ＋ | 低保真§23 `2026-09-19` 行 | 022/023 |

无第四 ID、无「顺手」空白/格式/历史日志改写、无测试/源码/HTML 行。归因 PASS。

---

## 5. 派发核项（命令证据）

### 5.1 021 删除后与 PGM L658/L669、AccessService 1305–1313

PGM 工作区 = after 树。L658：「HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板，无需逐人配权限。」L669：「职级影响默认权限模板和审批层级，但不能替代功能权限点。」

after 树 `AccessService.java` 1305–1313（blob `3720af8c…`，未改）：

```
1305                        // M006-G-02（PGM-1.10.1/1.10.2）：已定岗员工改职级 = 按新职级的岗位映射重算派生角色/模板，旧派生同步失效；
1306                        // 走与定岗相同的路径（含高危审批闸），无映射时由 resolvePositionGrant 明确拒绝，不静默保留旧模板。
1307                        UserPositionAssignment position = repository.findUserPositionAssignment(userId).orElse(null);
1308                        StaffLevelAssignment saved;
1309                        if (position != null && !request.staffLevelId().equals(position.staffLevelId())) {
1310                            saved = assignUserPositionDirect(new UserPositionAssignmentRequest(
1311                                    request.idempotencyKey(), request.operatorNote(), userId, position.positionCode(),
1312                                    request.staffLevelId(), request.baseSalaryAmountCent(), currency, request.salaryDetail()), true)
1313                                    .staffLevelAssignment();
```

AccessController after 树 :262 仍为 `PUT /users/{id}/staff-level`。

对照：before F9 把「仅职级变化、责任范围变化」排除出触发，与 L658 自动适配及 1305–1313 改职级重算相反；before 异常格「阻止保存或要求高级审批」以职级挡位替代功能权限点，与 L669 相反。after 去掉该排除、异常格改为「职级不替代功能权限点；敏感权限授出仍按五类高危先审批」。与 HTML/代码现状一致。本 diff 未改 HTML、AccessService。

### 5.2 022 删后要求句仍在

findings 所引 before :748/:2700 原文：

`8. 未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属。（…详见 platform-governance-prd-v0.1.md §12 第 15 条。）`

after 因文首 +2、页面日志 +1、流程图日志 +1，该两句分别在 :750 / :2704，字节与 before :748/:2700 全等。after :748/:2700 不是该句，属插入下移，要求句未删。

PRD after 树未改。`platform-governance-prd-v0.1.md` :657 第 15 条仍为强制要求「所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合」。

补录 before :2992 = after :2997，仍标「【历史摘录，现行时态已更新】…所有对象继续携带综合体项目归属。」定稿写明不改。after 中「所有对象继续携带」仅余：说明行 8、页面日志 782、低保真日志 2733、补录 2997。假完成声明已从后续细化 11/12 删除。

### 5.3 023 与废止行、ENUM 六值、不重开 009

after :930（before :927）仍为「【已废止/不得回灌】业态编号…ENUM「商户业态」只有六值名称」。after :929 业态名称校验：「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他六值闭集」。BEQR :195「商户业态」：酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。页面列「业态名称、说明」与六值名称一致，未回灌编号、未写「业态代码」。字段字典§19 变更日志仍保留 2026-09-11 对 009 的历史行，本批不加行、不重开 009。

### 5.4 未动面

三份业务 HTML、锁、sidecar、主索引、HEAD、backend 树、OpenAPI、集团 PRD、验收包、平台治理 PRD、绑定设计、merchant-space-contract-model、findings 队列：两树 blob 或路径清单已证 before=after。工作区三 HTML / AccessService / OpenAPI / 根 `index` / lock = after 树。

---

## 6. 对抗式（grilling 结构，不问人）

1. 若把拟改 5/6 当整行替换，会丢掉 V394 句。实物是子串删除「；所有对象继续携带 `project_id` 并按项目归属聚合」，V394 句仍在。不 FAIL。
2. 「第748/2700行」在 after 因插入下移。要求句全文仍在 :750/:2704。不 FAIL。
3. 职级未写入 F9 正向触发。定稿最小边界只删排除、不新写规则。不 FAIL。
4. 说明行/变更日志仍出现「仅职级变化」「所有对象继续携带」是在描述删除，不是回灌规则。F9 规则句与后续细化 11/12 已无排除/假完成。不 FAIL。
5. 补录仍带历史摘录。定稿明确不改。不 FAIL。
6. 字段字典未加 2026-09-19。符合派发。不 FAIL。
7. 真实索引仍 before blob。符合 B3 临时索引。不 FAIL。
8. findings 仍三是，未提前清队。符合 B4 前口径。不 FAIL。
9. 无无 ID 行、无第四文件、无源码/HTML/锁。不 FAIL。
10. 未造执行测试、未冒称 Java/HTTP/DB 实跑。不 FAIL。

无可归本批「是」ID 的残留缺陷，亦无无 ID 越界。不回 B2/B3。

---

## 7. 结论

修复内容：PASS。11 处拟改逐字落地；删后句子与表格单元完整；版本 V0.06→V0.07；变更记录日期 2026-09-19；不新造规则/日志/缺口闭合；021 与 PGM L658/L669 及 AccessService 1305–1313 一致；022 要求句仍在且 V394 句保留；023 与废止行、ENUM 六值一致且未重开 009。

范围归因：PASS。10 hunk、+12/−7 全部归 021/022/023（版本/说明/三张变更记录归本批 ID）；无无 ID 行；三 HTML/锁/主索引/代码/OpenAPI/其他文档未动。

总结论：PASS。本席不行使否决。已冻结判据不增判据。

---

## 8. 两个 tree 对象原文（`git cat-file -p`，TAB 分隔 object 与路径）

before `ad0dc4fc15b422440bb24109f489fea47510d639`（1673 bytes，SHA256 `3eb31d361159b381f79c4341e9730244ec05498ab794634d8a2c86d93251444f`，26 行 / 26 TAB）：

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
040000 tree 25dbc00ae26a12f1330153ad4dcc5c6b64cbb11c	docs
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

after `9a975ed491a93d3029f30e7cb415e0ec609b1487`（1673 bytes，SHA256 `68fe93d57d0e8882063f57ca5fc74efff76dea5f1a96fdd3da3ab072950b8613`，26 行 / 26 TAB）：与上逐行相同，唯一差异为 `docs` 树 `25dbc00ae26a12f1330153ad4dcc5c6b64cbb11c` → `a5223c1f22903cf97d17cb2ce84da5dce95e836b`。`backend` 树仍 `1ebe13e6d9ed813f92724d361fd46cea5432fe95`；根 `index` 仍 `65d675154f23ffb2d0196e017d44a5e7017550f5`。

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
040000 tree a5223c1f22903cf97d17cb2ce84da5dce95e836b	docs
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

完整原样 `git diff --binary` 见文末 §9（14429 bytes，SHA256 已核）。

## 9. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary ad0dc4fc15b422440bb24109f489fea47510d639 9a975ed491a93d3029f30e7cb415e0ec609b1487`

before tree = `ad0dc4fc15b422440bb24109f489fea47510d639`

after tree = `9a975ed491a93d3029f30e7cb415e0ec609b1487`

字节 14429；SHA256 `09c9e3f94125b63bc841b0c7493f9378956d315634033cefdfd50b779019e3d6`；UTF-8 完整原文如下。

````diff
diff --git a/docs/platform-governance-product-spec-v0.2.md b/docs/platform-governance-product-spec-v0.2.md
index ed8b55d1..70150ed9 100644
--- a/docs/platform-governance-product-spec-v0.2.md
+++ b/docs/platform-governance-product-spec-v0.2.md
@@ -1,10 +1,12 @@
-# 平台治理产品规格 V0.06
+# 平台治理产品规格 V0.07
 > 2026-07-04 逻辑一致性修订：§9.6 准入落库口径收口为 supervisor_review + supervisor_review_decisions（admission_audits/contract_admission_rules 从未建）；§14A 终端开关职责边界脚注；补名词四层脚注
 
 > 2026-09-01 M01 全量复审：按三份现行 HTML 补齐金额精度、合同本地 AI、合同当地时间和外部经销商治理的跨篇规格；删除已判称谓切片、复制模板现行支路，并统一集团视图已承接时态。
 
 > 2026-09-11 160 全量审计 M014（B3-r1，V0.05→V0.06）：R5 标注补 2026-09-11 现状行（005）；名词脚注权威回到三份 HTML + §0A（006）；空间类型去「项目」并注明归属根（001）、物理实现括注改字典活校验（002）；私人手机 App 能力按 HTML 低风险功能闭集（003）；集团视图审批属性两处改 V394 已闭合（004）；合同状态迁移图删除改成员说明（007）；§24.1 标题路径订正（008）；「业态编号」跟随补录标废止、业态名称括注 media（009）；第十一端产品码统一 dealer_portal 并标未实现（010）；终端开关页三处收口（011）；总览图补 F11（012）；F3 图非法行改注释（013）；补录 admission_audits 句标废止（014）；低保真一级导航表头改分组（015）；四处修订说明的指纹定义改现行（016）；merchant_credentials 改 V028 合规到期台账（017）；§9.6 悬空引用订正（018）。
 
+> 2026-09-19 160 全量审计 M014（B3-r2，V0.06→V0.07）：F9 强制补充规则去掉与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的仅职级变化、责任范围变化排除，异常分支不再以职级挡位替代功能权限点（021）；页面清单 §18 与低保真 §21 删去「所有对象继续携带 project_id 并按项目归属聚合」假完成声明（022）；业态列表去掉已废止的「编号」列（023）。
+
 > **⚠️ R5 业务HTML缺口审计修正标注（2026-06-18，只读审计后回填，未改代码）**
 > 以业务说明地图为基准核验真实迁移(V001–V065)+代码，本规格涉及的两条治理链与文档声称不符：
 > - **合同准入卡控+分级放行（HTML #contract/#flow 核心链）**：`SupervisorReviewService.evaluate` 是孤立 HTTP 评估接口，票务/酒吧/游乐/结算等业务域**在动作前零调用**它，"超范围→卡住→升级→放行"闸口**不触发**；`AdmissionAuditsPage` 仅聚合 merchants/bindings/contracts 三列表做伪闭环。`contract_admission_rules`/`admission_audits` 两表**从未建**，审批层级实际落 `supervisor_review_decisions`(V057)——本规格 §9.6"合同准入规则页"与"admission_audits.approval_level"表述需收口。
@@ -229,7 +231,7 @@
 
 | 区块 | 内容 |
 |---|---|
-| 业态列表 | 业态名称、编号、说明 |
+| 业态列表 | 业态名称、说明 |
 | 业态详情 | 业态说明、引用关系 |
 | 引用关系 | 被哪些商户、合同、空间引用 |
 
@@ -761,7 +763,7 @@ P0 页面清单完成后，后续原型和开发应至少满足以下产品验
 8. 权限点首批由平台统一预置，实施阶段允许边配置边补充。
 9. 操作留痕支持附件或证据引用；高风险操作必须支持。
 10. 合同过期、空间/业态超范围一律先阻断；阻断后未命中红线的可按层级升级审批并留痕。必填证照缺失一律阻断；证照或资质到期后可申请后台可配时长的宽限期（业务参数，按证照类型可配，须同时配置上下限），宽限期须经最高权限审批后生效，期内可继续经营但每日提醒，超过宽限期仍未更新的一律阻断且不得再次延长。
-11. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归属聚合。`analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD V1.01 §4），不反向改写业务口径。
+11. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地。`analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD V1.01 §4），不反向改写业务口径。
 
 ## 19. 本版结论
 
@@ -777,6 +779,7 @@ P0 页面清单完成后，后续原型和开发应至少满足以下产品验
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-19 | 改 | §7.3 / §18 后续细化 11 | 版本升 V0.07（合并文件）；业态列表去掉已废止的「编号」列；删去与主体档案、四类主体绑定表实况不符的「所有对象继续携带 project_id 并按项目归属聚合」 | 160 全量审计 M014-022/023；ENUM 商户业态六值名称；四类主体绑定开发设计 V0.13 §3 |
 | 2026-09-11 | 改 | 名词脚注 / §6.3 / §9.6 / §10.6 修订说明 / §14A.3 / §18 | 权威实体关系回到三份 HTML + §0A；空间树首项注归属根；悬空引用「§2999」订正；修订说明指纹定义改现行；开关列表去未定义「状态」列；集团视图审批属性改 V394 已闭合 | 160 全量审计 M014-001/004/006/011/016/018 |
 | 2026-09-01 | 增/改 | §0A / 名词 / §18 | 补四类 HTML 现行规则的页面与交互承接；去已判称谓切片；集团视图改为已承接 | M01 全量复审 ADV-01/02/04/05/07/08 |
 | 2026-08-27 | 改 | 合同审批 / 空间状态 / 商户类型 / 业态 / 设备凭证绑定 | 合同生效改为主线（默认总经理＋逐级自动补全）＋业务/财务/法务平行线全部通过；空间六态与状态机改为不定义；商户类型收口为合作模式六类；删创建业态与换机待审；业态页六值闭集 | P2 M01 第二整轮 ①②③④⑤⑥；PGM 合同审批链、ENUM 合作模式/商户业态 |
@@ -1976,7 +1979,7 @@ flowchart TD
 - 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、税费规则与税费调整五类权限不得沿图中“直接授权”路径即时生效，必须先生成授权申请并按上一级、上两级或最高权限完成审批。
 - 授权申请人不得审批自己的申请；同一主体同时命中申请人与审批人时，审批动作拒绝并留痕。
 - 高危强制范围不能通过后台开关关闭；配置只能新增高危权限或提高审批等级。
-- 保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效，审批通过后方可生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件。
+- 保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效，审批通过后方可生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把任意手工模板应用列为触发条件。
 
 ### 11.5 异常分支
 
@@ -1987,7 +1990,7 @@ flowchart TD
 | 单次有效模板已消耗 | 阻止再次使用，并提示消耗记录 |
 | 同一权限同时被授予和显式拒绝 | 按显式拒绝处理，并展示冲突来源 |
 | 敏感权限未填写授权原因 | 要求补充原因 |
-| 职级与敏感权限不匹配 | 阻止保存或要求高级审批 |
+| 职级与敏感权限不匹配 | 职级不替代功能权限点；敏感权限授出仍按五类高危先审批 |
 | 员工离职或停岗 | 立即阻断登录和业务操作，并使角色、模板、临时授权和员工单独授权失效；写收权留痕 |
 | 保存岗位、职责或同一商户、同一项目范围内的所属组织变更 | 立即按新岗位模板重算，旧岗位或旧范围派生权限同步失效；同一范围内普通模板权限随保存立即生效；超出新模板范围的增权保持未生效直至审批通过；需保留的员工单独授权超出新范围则暂停并重新审批 |
 | 保存所属商户或发生跨项目变更 | 旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批 |
@@ -2228,6 +2231,7 @@ flowchart TD
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-19 | 改 | §11.4 F9 强制补充规则 / §11.5 异常分支 | 删去与治理 HTML「HR 定层级/职位即自动适配」「职级影响默认权限模板」相反的仅职级变化、责任范围变化排除，只保留任意手工模板应用不触发；职级与敏感权限不匹配改为不以职级替代功能权限点 | 160 全量审计 M014-021；PGM L658/L669；AccessService 改职级重算现状 |
 | 2026-09-11 | 改 | §2 总览图 / §5 F3 / §8 修订说明 | 总览关系图补 F11；F3 合同状态迁移图删除改成员说明、正常流程图非法行改注释；修订说明指纹定义改现行 | 160 全量审计 M014-007/012/013/016 |
 | 2026-09-01 | 改 | §0A / F9 | 补四类 HTML 现行规则的流程承接；删除已废止复制模板支路 | M01 全量复审 ADV-01/02/04/05/06 |
 | 2026-08-27 | 改 | 合同审批 / 空间状态 / 商户类型 / 业态 / 设备凭证绑定 | 合同生效改为主线（默认总经理＋逐级自动补全）＋业务/财务/法务平行线全部通过；空间六态与状态机改为不定义；商户类型收口为合作模式六类；删创建业态与换机待审；业态页六值闭集 | P2 M01 第二整轮 ①②③④⑤⑥；PGM 合同审批链、ENUM 合作模式/商户业态 |
@@ -2412,7 +2416,7 @@ P0 大多数页面采用“筛选区 + 列表 + 详情入口 + 批量/单项操
 
 | 区域 | 低保真内容 |
 |---|---|
-| 业态列表 | 业态名称、编号、说明 |
+| 业态列表 | 业态名称、说明 |
 | 业态详情 | 说明、引用关系 |
 | 引用关系 | 关联商户、合同、空间数量 |
 
@@ -2714,7 +2718,7 @@ P0 大多数页面采用“筛选区 + 列表 + 详情入口 + 批量/单项操
 9. 合同过期、空间/业态超范围一律先阻断；阻断后未命中红线的可按层级升级审批并留痕。必填证照缺失一律阻断；证照或资质到期后可申请后台可配时长的宽限期（业务参数，按证照类型可配，须同时配置上下限），宽限期须经最高权限审批后生效，期内可继续经营但每日提醒，超过宽限期仍未更新的一律阻断且不得再次延长。
 10. 权限点首批由平台统一预置，实施阶段允许边配置边补充。
 11. 权限模板在角色权限页承接，支持角色级权限模板（基础模板、有效临时模板）；临时授权可设有效期或单次使用。【已废止/不得回灌】复制模板作为第三种模板种类；有效期/单次使用只给临时授权。不另拆孤立页面。
-12. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归属聚合。`analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD V1.01 §4），不反向改写业务口径。
+12. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地。`analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD V1.01 §4），不反向改写业务口径。
 
 ## 22. 本版结论
 
@@ -2726,6 +2730,7 @@ P0 大多数页面采用“筛选区 + 列表 + 详情入口 + 批量/单项操
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-19 | 改 | §8.1 / §21 后续细化 12 | 业态列表去掉已废止的「编号」列；删去与主体档案、四类主体绑定表实况不符的「所有对象继续携带 project_id 并按项目归属聚合」 | 160 全量审计 M014-022/023；ENUM 商户业态六值名称；四类主体绑定开发设计 V0.13 §3 |
 | 2026-09-11 | 改 | §3.1 / §12 / §17A.1 / §17A.2 / §19 / §21 | 版本升 V0.06（合并文件）；一级导航表头改分组；修订说明指纹定义改现行；开关列表去生效/失效列口径、已废止校验句标注；私人手机 App 能力按 HTML 低风险功能闭集；集团视图审批属性改 V394 已闭合 | 160 全量审计 M014-003/004/011/015/016 |
 | 2026-09-01 | 增/改 | §0A / §21 | 补四类 HTML 现行规则的低保真承接；集团视图改为已承接 | M01 全量复审 ADV-01/02/04/05/08 |
 | 2026-08-27 | 改 | 合同审批 / 空间状态 / 商户类型 / 业态 / 设备凭证绑定 | 合同生效改为主线（默认总经理＋逐级自动补全）＋业务/财务/法务平行线全部通过；空间六态与状态机改为不定义；商户类型收口为合作模式六类；删创建业态与换机待审；业态页六值闭集 | P2 M01 第二整轮 ①②③④⑤⑥；PGM 合同审批链、ENUM 合作模式/商户业态 |
````
