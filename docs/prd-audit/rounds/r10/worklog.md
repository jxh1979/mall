# R10 修复主批 工作台账

分支 `docs/r10-verification-fixes`，基线 `origin/main = c28215b6`。边做边写。
修改器（`~/r10-scripts/apply.py` + 各批 `e*.py` / `p*.py`）与校验方（`~/r10-scripts/scan2.mjs`、`assert.mjs`、`probe*.mjs`、仓库门禁）**两套独立实现**。

## 开工基线（已核）

- 三份 HTML 哈希（本轮必须零字节改动）：
  - `924fb4c6ffb0ebbdaccccaf4220f0cf97fadc88563997458f1b402cbb3a50ecc`  business-system-map-for-operations-v0.1.html
  - `6cf2e4718e68d79980d907c9aee14c431f9d0928ea78ee23d400288f1cdb527c`  platform-governance-map-for-operations-v0.1.html
  - `13a896685bc03cea5deec46c77180e070d36304b3a549d883eedef5e60188462`  business-enum-quick-reference-v0.1.html
- worktree `~/wt-r10` 建立，HEAD=c28215b6，node_modules 软链已建。

## 历史留痕排除口径（沿用 R9 门禁 EXCLUDE，本轮实测后追加 4 项）

`changelog | change-log | -report | .report. | diff-matrix | anchor-rebuild | html-map | reanchor | html-mirror-scan | history-index | /rounds/ | /_archive/ | business-decision-ledger | business-decision-log | backup- | FULL-SENTENCE-DIFF | **FULL_SENTENCE_DIFF**（下划线写法，R9 正则漏网） | FULL-DIFF | **appendix-deep-**（深度审计发现台账） | **HTML-PASS-**（2026-07-28 HTML 通过审计快照）`

另按 R9 已定原则不改写的两类：
1. 各 PRD 文内「变更历史 / 变更日志」表格行（形如 `| 2026-08-04 | 改 | …`）——记录当时事实；
2. `docs/prd-audit/authority-rule-ledger.csv` 的 `source_text` 列——HTML 原文抄录，改动即触发门禁⑤。
   例外：该文件 `PGM-0054`／`PGM-0056` 两行的**句子类列**引文仍写「保证金」，与 HTML 现文「押金」不符，属 R8 抽取残留漂移，**本轮未动**（见「未解决」）。

---

# P1 深层残留清扫

## 开工基线扫描（全库 md/json/yaml/csv/html，排除上述留痕）

| 词 | 在检命中(处/行) |
|---|---|
| 人工放行 | 216 / 178（其中 58 行 R8 已处置） |
| 保证金 | 66 / 60 |
| 场次容量 | 45 / 40 |
| 核销预检 | 18 / 17 |
| 票面原价 | 22 / 18 |
| 统一入场刷脸闸 | 3 / 3 |

## 执行批次

| 批 | 脚本 | 内容 | 结果 |
|---|---|---|---|
| e1 | `e1.py` | 「人工放行」28 份文件（amusement 开发设计与 openapi 除外） | 73 处替换 + 28 份文首口径块 |
| e2 | `e2.py` | `p0-end-to-end` **AC-AMU-02 整例改写为「系统不提供人工放行」反向验收** | 6 行 → 12 行 |
| e3 | `e3.py` | `amusement-operations-development-design.md`：人工放行 21 处 + 场次容量/三维度容量 10 处；§6.5「人工放行流程」整节废止、§6.4「三维度容量校验规则」整块改写为单维+双阈值 | 31 处 + 2 整块 + 口径块 |
| e4 | `e4.py` | openapi **yaml/json 成对改** 21 处/份：`manual-overrides` 两 operation、`verifications/preview` operation 加 `deprecated: true` 与废止 description；三维容量描述、票面原价→各业态价格（原称括注）、保证金→押金 | 42 处；`verify:openapi` deep-equal PASS |
| e5 | `e5.py` | 「保证金→押金」39 处（机读枚举值/表名/字段名一律不动）+ `requirements-code-implementation-matrix.csv` REQ-33 | 39 处 |
| e6 | `e6.py` | 「场次容量→项目容量（实时在玩人数）」30 处；p2 包场契约按 amusement PRD §11A.3 现行口径改（包场＝整体停止对外核销，不是占用整场次容量） | 30 处 + 14 份口径块 |
| e7 | `e7.py` | 票务「核销预检」两步流 23 处（酒吧年龄核验预检/运行配置预检/分红预检明确排除在外） | 23 处 + 5 份口径块 |
| e8 | `e8.py` | 「统一入场刷脸闸／人脸闸／入场闸」→ 综合体入场四词体系 4 处；`prd-audit-fix-change-log` L313「实际发放数」加 H-05 更正括注 | 6 处 |
| e9 + p1fix + p1fix2 | 同名脚本 | 断言暴露的残留：REQ-46 需求条正文、`matrix-migration-claims.json` V083 note、amusement 设计 §7 修订说明、追溯矩阵 R7 上修行、amusement PRD L7 校验顺序句 | 5 处 |

改法一律沿用 R8 已建立的模式：**文首口径块（引 ENUM/BSM 条目名 + DEC 编号）＋ 就地废止标注**；已实现的端点/权限点/事件/状态取值一律标「待代码轮下线，不得新增调用、不得新写入」。

## 收工断言（`node ~/r10-scripts/assert.mjs`，EXIT=0）

| 词 | 在检命中行 | 已带废止/更正标注 | 变更历史行(不改) | 只读源(HTML/账本) | **残留** |
|---|---|---|---|---|---|
| 人工放行 | 211 | 197 | 10 | 4 | **0** |
| 保证金 | 7 | 2 | 3 | 2 | **0** |
| 场次容量 | 52 | 51 | 1 | 0 | **0** |
| 核销预检 | 18 | 18 | 0 | 0 | **0** |
| preview_passed | 8 | 8 | 0 | 0 | **0** |
| 票面原价 | 15 | 14 | 1 | 0 | **0** |
| 统一入场刷脸闸 | 3 | 3 | 0 | 0 | **0** |
| 统一入场人脸闸 | 2 | 2 | 0 | 0 | **0** |
| 统一入场闸 | 1 | 1 | 0 | 0 | **0** |
| 三维度容量 | 11 | 7 | 4 | 0 | **0** |
| 三维容量 | 14 | 13 | 1 | 0 | **0** |

openapi 生成物已重生成并自检：`docs:operation-matrix`（720 operations）、`docs:openapi-closure`（720 operations / 328 permissions / 1440 events），`verify:p0-contract` 全链 EXIT=0。

---

# P2 PRD 同步（照抄现行 HTML 口径）

| 项 | 落点 | 内容 |
|---|---|---|
| M-01 | `member-and-tier-product-prd-v1.0.md` §8.3 / §12 第 5 条 / §13 反例表 | 降级预警口径同步；**L269 反向句「阻断降级」改为「不阻断：降级照常执行并在生效后立即补发通知」**（BSM「等级与成长值」条） |
| M-02 | `ticketing-entitlement-checkin-product-prd-v0.1.md` §7.1「按场次」行 / §8.3.1 | 删「只在当前场次有效／过场即作废」旧句，换 BSM「核销单位」条的**场次终结制**：绑定当前场次、不得开启另一场次、同场不限次进出且不重复扣次、权益仅限当场消耗、场次结束即终结按域归属；**多次票剩余入场次数跨场次保留** |
| M-03 | `settlement-rebuild-prd-v1.0.md` L101、`finance-settlement-product-prd-v0.1.md` L107 | 删「或权重为零」 |
| M-04 | `settlement-rebuild-prd-v1.0.md` §3 第 2 条 | 域归属补第四档**餐饮域与零售域 → 城市项目公司**；并补「本条只适用于从未完成检票环节的票」与场次终结制的分流 |
| M-05 | `terminology-canonical-v1.0.md` | L61「未核销归项目」→「未核销按域归属」；**新登记四个名词**：域归属对照、通票未核销减除额、场次终结制、综合体入场四词 |
| M-06 | `finance-settlement-product-prd-v0.1.md` §3A | 新增承接块：同一张域归属对照表、两类钱各自单独立项、**四栏分列（减除额/到期未核销/未分配池余额/金额尾差）不得合并**、减除额计费基数按第二段参与集合 |
| M-07 | `amusement-business-operations-product-prd-v0.1.md` §9 前 | 新增承接块：游乐未核销款归游乐公司（域归属对照总规则在游乐票上的适用）；**游乐场次 ≠ 票务场次窗口**，两处「场次」不得混用 |
| V6 A-1 | `business-operations-layer-spec-v0.2.md` L66/L599/L1828/L1842 | 四处「场次、容量」口语并列同 L81 改法 → 「项目容量（实时在玩人数）」 |
| — | `business-decision-log-v0.1.md` L66 | 「具体核销放行以场次容量为主控口径」加【**已作废**（R8 用户裁决 D-02 / DEC-R8-04）】括注（同 R8-U-06 先例，只加注不改原文） |

门禁反馈的 3 处错引已订正：`BSM「主要收入归属」` → `ENUM「主要收入归属」`（该条在 ENUM-E0114，不在 BSM）。

---

# P3 登记与台账体系

## P3-1 决策台账 `docs/prd-audit/business-decision-ledger.md`

- **DEC-R8-17 行「本轮未做迁移」更正**：加【2026-08-07 R10 更正：迁移已于 R9 实施完毕（PR #341 → main c28215b6）】，引 R9 worklog §T1~T4、`ledger-source-refresh.csv`（197 行）为证。
- **L1527「用户未裁」更正块**：`face.verification.enabled` 键名——用户已于 2026-08-06 裁决永久沿用，原「登记为待代码轮单独裁定」取消。
- **新增「R9／R10 落地自证回流」节**：R9 T9 落盘 sha256 三值（与 R10 开工/收工逐字一致）、落点清单、合入记录、收工自检；R10 本轮落地自证。
- **补两条正式编号**（对用户已作出裁决的编号化，非新裁决）：
  - **DEC-R8-39**（2026-08-06）：`face.verification.enabled` 键名永久沿用、不改；不设待代码轮改键名的待办。
  - **DEC-R8-40**（2026-08-07）：商户专区「费用通知接收人手机号」不脱敏，援引 **DEC-R7-33** 先例（脱敏项为封闭清单、没有第八项）。
- 该文件 HEAD 行尾混合（1452 CRLF + 76 LF），改动以 `p3c2.py` **逐字节重放**，diff 为 41 增 1 改。

## P3-2 `docs/prd-audit-fix-change-log-20260804.md`

- **GAP-04 草案编号 → 正式编号**（依 R9 worklog §T3 对照表）：21→20、26/27→24/25、23/24→21/22（明细行 4 处 + 汇总行 1 处，原草案编号一律括注保留）；
- **「覆盖家数取表」→「第二段参与家数取表」**（DEC-R8-37：「参与家数档位表」原称「覆盖家数档位表」，同一张表两个叫法）；
- **GAP-21 与 GAP-28 去重**：GAP-21 主表行与状态表行均标「终态：已并入 GAP-28，本条不再单独跟踪」；
- **GAP-23 / GAP-35 状态同步 `fixed-in-r9`**（主表 + 状态表各 2 处），并写明核实依据。

## P3-3 `docs/prd-audit/rounds/r8/findings.csv`

- `R9-B-01` → **`resolved`**（2026-08-07 裁不脱敏，DEC-R8-40）；
- `R8-U-05` → **`fixed-in-r10`**，注明 P2 已补 4 处并给出全库口径；
- 新增 3 行 `open`：
  - `R10-O-01` REQ-110 能源费口径代码轮收敛；
  - `R10-O-02` `tools/generate-partial-requirement-mock-coverage.mjs` R9 越界改动待终审确认；
  - `R10-O-03` 「接入异常已忽略」态已裁（删，与差异/回流同规则）、待代码轮实施。
- 本轮 P1/P2 修复的登记随上述两行 disposition 一并写入。

## P3-4 本地票与 UI 验收矩阵

- `docs/full-admin-ui-acceptance-matrix-v0.1.md` **UI-ADM-MER-007 备注「待裁」改已裁事实**（引 DEC-R8-40 / DEC-R7-33，明确展示层无须改造、e2e 断言不变）。
- **本地票 14 头 Status→resolved 未执行**：`.scratch/prd-audit-r8/issues/14-*.md` 在 `~/complex-management`、`~/wt-r10` 与全盘搜索均**不存在**（R9 的临时目录已清）。见「未解决」。

## P3-5 账本与承接矩阵

- **两条误判 ORPHANED 重锚**（R10-01）：
  - `BSM-0394` ORPHANED/「旧记 L1491」→ **「演出票」/ L1549**（`BSM-E0577`，唯一命中）；
  - `PGM-0106` ORPHANED/「旧记 L644」→ **「触摸屏一体机」/ L660**（`PGM-E0200`，3 候选中按 source_text 逐字匹配定唯一）；
  - 承接矩阵同名同步改，门禁④由此从 FAIL 转 PASS。
- **承接矩阵补 6 行**（R10-02，`prd_path` 一律指向具体小节，不用全文/综合条款）：
  | rule_id | entry_name | prd_path | 小节 |
  |---|---|---|---|
  | BSM-0542 | 进场（综合体入场） | ticketing-entitlement-checkin PRD | 8.3 现场核销（三道环节） |
  | BSM-0543 | 通票未核销减除额 | finance-settlement PRD | 3A · 按域归属对照归属（四栏分列） |
  | BEQ-0168 | 域归属对照 | finance-settlement PRD | 3A · 同一张对照表 |
  | BSM-0544 | 酒吧通票可分配池的池金额，是该票扣完第三方渠道费用 | settlement-rebuild PRD | 5.1 分账两问 |
  | BSM-0545 | 到期处理 | settlement-rebuild PRD | 3 核心设计原则第 2 条 |
  | BSM-0546 | 等级与成长值 | member-and-tier PRD | 8.3 降级评估与通知 |

## P3-6 引用规范

- **8 处多义/悬空引用改注册表原名**（R10-03）：`business-dashboard-metrics` ×2（PGM「所有导出的文件一律带密码……」→ 全名）、`platform-governance-prd` ×2（BSM「注册、提交资料审批……」→ BSM「员工私人手机 App」）、`settlement-rebuild` ×3（省略号/超长整句 → BSM「酒吧通票可分配池的池金额，是该票扣完第三方渠道费用」/ BSM「官方和第三方渠道门票售出后原则上不退款；未到场」）、`service-order` ×1（引号嵌套畸形 → BSM「本章的「点单员」指帮客人下单、向客人出示收款码」）。
- **中文别名改代号写法：实测 36 处 / 6 份文件**（任务书给的是 19 处，如实记录差异）：
  `业务系统地图「」→BSM「」`、`平台治理地图「」/治理地图「」→PGM「」`、`业务枚举速查表「」/业务枚举速查「」/业务速查表「」/枚举「」→ENUM「」`。
  历史留痕流水（`business-decision-log` L416/L909、`business-decision-ledger` L1218）**不动**。
  改后门禁抓出 8 处定位不到/多义，逐处订正为可唯一定位的注册表原名或章节名（如 `PGM「综合体/空间/商户/合同」` → `PGM「商户和主体」+ PGM「合同和准入」`、`BSM「客人端」`(7 义) → `BSM「客人端·主要功能」`、`ENUM「财务结算与对账」`(2 义) → `ENUM「六、财务结算与对账」`）。

---

# P4 门禁方向性检查

`tools/verify-entry-anchors.mjs` 新增**检查⑥：账本每行必须在承接矩阵有对应行**。
病根：原④只检「矩阵→账本」，缺「账本→矩阵」这一向，账本新增条目可长期不进矩阵而门禁不响（本轮开工时实测正有 6 行处于该状态）。
实现含**显式伪 ID 白名单 9 条**（`{BSM,PGM,BEQ}-MPS-000{1,2,3}`，member-points 重建 Phase4 占位），并新增反向断言：矩阵里出现账本没有、又不在白名单的 rule_id 同样判红。

| 测试 | 操作 | 结果 |
|---|---|---|
| 正向 | 不改动 | PASS，EXIT=0 |
| 反向 A | 删矩阵中 `BSM-0543` 一行 | FAIL「账本行未登记进承接矩阵：BSM-0543「通票未核销减除额」（检查⑥）」，**EXIT=1** |
| 反向 B | 矩阵加 `BSM-FAKE-9999,NOT_IN_LEDGER,…` | FAIL「承接矩阵出现账本没有、又不在伪 ID 白名单里的 rule_id：BSM-FAKE-9999（检查⑥）」，**EXIT=1** |
| 恢复 | 还原矩阵 | PASS，**EXIT=0** |

---

# 收工核对

- **三份 HTML 收工 sha256**：`13a89668…` / `924fb4c6…` / `6cf2e471…`，与开工基线**逐字一致，零字节改动**。
- `npm run verify:predeploy` → **EXIT=0**（全链 15 道门禁）。
- `node ~/r10-scripts/assert.mjs`（P1 十一词残留断言）→ **EXIT=0**，残留 0。
- `git diff --shortstat`：**72 files changed, 537 insertions(+), 326 deletions(-)**；未跟踪仅 `docs/prd-audit/rounds/r10/`。
- **本轮未执行** `git commit` / `git push` / PR。

## 过程中发现并自纠的一项工程事故

`p3a.py` / `p3c.py` 等 Python 批用 `io.open` 读写，把原本 **CRLF 行尾**的文件整体写成 LF，造成 6 份文件出现全文件级 diff（`business-operations-layer-spec` 1969/1969、`business-decision-log` 910/910 等）。
已用 `fixeol.py` 逐文件比对 HEAD 行尾形态并转回 CRLF（5 份纯 CRLF 文件）；`business-decision-ledger.md` 因 HEAD 行尾混合（1452 CRLF + 76 LF）无法机械转换，改用 `p3c2.py` 从 HEAD 版本**逐字节重放**三处改动。
修复后 diff 规模从 5000+ 行降回 537/326，最大单文件 52/43。


---

# R10 第四批（HTML 修订批）实施记录 — 2026-08-06

依据：`prd-audit-r8/HTML修改建议草案-第四批.md` §F（33 落点终稿，唯一落笔依据）。
范围追加：草案 §K 扩登记优先清单 U-1~U-12 全批、本轮就登（编排者 2026-08-06 转达用户裁决）。
开工基线核对：三份 HTML sha256 前 8 位 `13a89668` / `924fb4c6` / `6cf2e471`，行数 256 / 1881 / 889 —— **与任务书基线逐字一致**。
worktree `~/wt-r10` 分支 `docs/r10-verification-fixes`，开工时已有 C-R10 主批未提交改动 73 项（72 tracked ＋ 1 untracked 目录），本批只叠加。

## T1 三份 HTML ＋ spec 落笔（33 落点 / 36 处字节替换）

脚本 `~/r10b4_html.py`：字节级读写，每处断言原文**恰好命中 1 次**，任一处不满足即整体中止不写盘；写盘前断言行数与 CRLF/LF 计数不变。

- 落点 11（3 子项）合为 2 处替换、落点 15（正文＋末句）2 处、落点 28（首句＋顺延一）2 处，故 33 落点 ＝ 36 处替换。
- 同行多落点一次改完：ENUM L49×5、ENUM L207×2、BSM L751×3、BSM L796×2、BSM L1218×3、BSM L1605×1。
- 落点 5 按 §F 注记，把原「每一行的百分比合计必须等于 100%……」并入新段，**未留两份**。
- 结果：ENUM 95858→99834 字节、BSM 250028→254513、PGM 87112→87238、spec 62234→62451；**四份文件行数与行尾形态全部不变（LF，CRLF＝0）**。

## T2 锚点三表同步

- `entry-registry.csv`：4 条指纹重取（BSM-E0071 / BSM-E0163 / PGM-E0216 / ENUM-E0018）；BSM-E0071 条目名含已删除的「场次、容量」字样，按注册表既有取名惯例（≤24 字、切在「、」边界）改为「管理游乐项目、开放时间」，全库 `.md` 对该旧名引用实测 0 处。
- `authority-rule-ledger.csv`：17 条 `source_text` 按与 HTML **完全相同的（旧,新）逐字替换**重取（不改写）；其中 4 行（BSM-0310 / BSM-0415 / BEQ-0118 / PGM-0117）另有 trigger / preconditions / approval / value_flow 等列存着同一段正文的复制品（账本重建遗留，全库 357/907 行有此现象），同步一并更新，避免旧话在账本里残留。
- **自纠一处**：ENUM 落点 6 的原句与 BSM L751 逐字相同，首版脚本未按 `html_file` 限定，误把 BSM-0544 的试算基准改成 ENUM 新话（BSM 侧本批不改）——已用 `~/r10b4_fix.py` 原文回改，并加全表扫描确认「跨文件误植 0、旧话残留 0（2 处报警为插入型落点的前缀自命中，非残留）」。
- 三份 HTML 行数未变，`line_now` 无须刷新。
- `node tools/verify-entry-anchors.mjs` → **PASS，EXIT=0**（注册表 1210、账本 907、承接矩阵 916）。

## T3 下游 PRD 同步（§J.2，8 份 34 处 ＋ 5 条变更日志）

| PRD | 落地条目 |
|---|---|
| `settlement-rebuild-prd-v1.0.md` | H-21 两步减除改一步（前置 1／前置 2 表两行 ＋ 守恒式 ＋ U-2 断言）、H-22 第一段配置共用通告与版本固化、矛盾2（无人可分时＋三栏＋末句限定）、矛盾4 最低可能池金额、矛盾5 扩表制（去 10 家上限、缺行暂挂、补配续算）、U-6 用例改写、缺口清单补 ⑤⑥、能力表口径 ＋ H-24 到期未核销改人份 |
| `dividend-engine-prd-v0.1.md` | H-25 预览改事件驱动（§2#3 ＋ §9，删 08:00 与「07:00 闭店即数据闭合」推理，明写覆盖 DEC-R8-15）、矛盾19 月粒度（§2#3 ＋ §4）、矛盾16 发放付账日顺延口径（§7） |
| `ticketing-entitlement-checkin-product-prd-v0.1.md` | H-24（§7 到期生命周期 ＋ §8.3.1 场次终结制人份分界）、矛盾25 一晚绑营业日、矛盾24 受赠人打开页提示 |
| `leasing-acquisition-prd-v1.0.md` | H-23／矛盾7／矛盾9 交叉引用补新口径要点（正文仍归 real-estate PRD） |
| `real-estate-full-chain-domain-prd-v0.1.md` | H-23 押金 30 天底线制整段改写、矛盾7 只乘月租、矛盾9 欠缴月数按金额折算、罚款同权重与 §G-2／§G-4 知情接受留档 |
| `finance-settlement-product-prd-v0.1.md` | 矛盾13 冲正原额（§13.1）、矛盾14 抽成异议期长度与起算（§13.1）、矛盾16 顺延统一（§4B.1 两处）、矛盾17 顺序顺延新增一条 ＋ 保存校验收窄（§4B.1 ＋ §4B 验收 ＋ §13.1） |
| `merchant-points-settlement-rules-v1.0.md` | 矛盾14 异议期起算、矛盾15 届满当日可付（含 §G-5 有意不写留档）、矛盾16／17 注块同步 |
| `r5-spec-T1-staff-tables-dev-design-20260618.md` | 矛盾12 ＋ 矛盾11 联动：新增「跨月／跨周／跨年班次的归属口径」小节（年度指标、节假日加班整班认定、营业日归口唯一例外、`work_date` 不构成归属月份） |

变更日志行：dividend / ticketing / leasing / finance / r5-spec 五份各补一行（settlement、real-estate、merchant-points 三份的变更日志不在文件末尾，本批未插行，改动已由决策台账承接）。

**范围说明（超出 §J.2 字面清单、但属「PRD 与新 HTML 现文一致」的必要同步，单列备查）**：
① settlement PRD §3.2 的到期未核销句原样抄自 ENUM「到期未核销款」条，H-24 改了主语后不同步即成旧话残留，故一并改；
② dividend PRD §7 的「节假日顺延」同理按矛盾16 统一。

## T4 附录 B 扩登记 U-1~U-12（编排者转达用户 2026-08-06 裁决：全批、本轮就登）

| # | 条目 | 处置 | 落点 |
|---|---|---|---|
| U-1 | PGM 开关变更四要素留痕 | 账本 `PGM-0194` ＋ 承接矩阵（MISSING）＋ findings `R10-U-01` open「零承接缺口」 | PGM 留痕与审计记录条 |
| U-2 | 数据隔离不提供关闭开关 | 账本 `PGM-0195` ＋ 矩阵（MISSING）＋ findings `R10-U-02` open | PGM 数据默认按项目隔离条 |
| U-3 | 全链可追溯全局原则 | 账本 `PGM-0196` ＋ 矩阵承接列如实标「无归口・零承接缺口」＋ findings `R10-U-03` open；**未硬造归口**，门禁未因此转红 | PGM 全局原则条 |
| U-4 | 配置只能承载规则 | 账本 `BSM-0547` ＋ 矩阵（MISSING）＋ findings `R10-U-04` open | BSM 会员规则计算条 |
| U-5 | 娱乐平台公司主体职责 | 账本 `PGM-0197` ＋ 矩阵（FULL）；`platform-governance-prd-v0.1.md` 补承接块，**逐字照抄 HTML 现文、无推断扩写** | PGM 娱乐平台公司条 |
| U-6 | 业态积分比例上下限缺 | **只登缺口** findings `R10-U-05` open；**不改 HTML**（补句须另出草案逐句批） | — |
| U-7 | 付账日顺延二 | 账本 `BSM-0548` ＋ 矩阵（FULL → finance PRD §4B.1）；**不改 HTML** | BSM 三个付账日条 |
| U-8 | 付账日顺延三 | 账本 `BSM-0549` ＋ 矩阵（FULL）；**不改 HTML** | 同上 |
| U-9 | 顺延不另行通知、以待付状态可见代替 | 账本 `BSM-0550` ＋ 矩阵（FULL）；**不改 HTML** | 同上 |
| U-10 | 跨年班次归属 | 随落点 25 落地后取**新现文**登记：账本 `BSM-0551` ＋ 矩阵（FULL → r5-spec §4） | BSM 请假扣薪条 |
| U-11 | 闭店时点与营业日边界 | 登记为「**有意不写**」：findings `R10-U-06` status `accepted-by-user` ＋ 决策台账 §G-3（含与 DEC-R8-60 锚点的耦合提示） | — |
| U-12 | 营业额抽成异议期长度与起算 | 随落点 11-c 落地后取**新现文**登记：账本 `BEQ-0169` ＋ 矩阵（FULL → finance PRD §13.1） | ENUM 营业额抽成模式条 |

**登记口径说明**：既有 907 行账本为「条目级」（`source_text` ＝整条正文）。本批 U-7~U-9 是同一条目内的三条独立规则，若各存一份整条正文既冗余又无法区分，故本批 10 行按 **§K.1 的规则级**登记——`source_text` 取该规则的**逐字原句**，`trigger` 逐条填实，其余分析列填 `NOT_STATED`。门禁检查⑤（source_text 须逐字存在于 HTML 现文）对句级同样有效，落盘前已逐条按原文片段比对（脚本 `~/r10b4_reg.py` 的 VERIFY 段，10/10 命中），**未只跑关键词**。

## T5 决策台账（纯追加）

- **DEC-R8-41 ~ DEC-R8-64**：本批 24 项裁决正式编号登记（H-21~H-26 六条、矛盾2/4/5/7/9/11 联动/12/13/14/15/16/17/19/20/24/25 十六条、附录A、附录B）。**DEC-R8-45 明写「覆盖 DEC-R8-15」**。
- **DEC-R8-65**：扩登记优先清单 U-1~U-12 全批、本轮就登；105+ 全量清单下轮出。
- **§G 五项有意接受**单独成节登记（轮换漏洞／罚款无防护／闭店无校验／低租金占比硬线／届满撞车不补），逐项写明**知情选择、日期、用户原话或理由、曾评估未采用的方案、日后若要改的改动面**，并明确「不构成待办、不进 open 缺陷池，下轮审计不得重报」。§G-3 另写明与 DEC-R8-60 的锚点耦合。
- 落地自证节内**登记了本批发现的一处跨文件口径差**（见 T6）。

## T6 本批发现、未擅改的一处跨文件口径差

**DEC-R8-48（矛盾4）只批了 ENUM 一处**：ENUM 的试算基准已改为「该票种在当前各项配置下的**最低可能**可分配池金额」，而 **BSM 可分配池池金额条的同一摘要句仍为「按该票种的可分配池金额试算」**——两份 HTML 在此点上现存表述差异。
§F 的 33 落点清单与「明确不动的位置」清单均未涉及该 BSM 句，**故本轮不擅自扩大改动范围**：
- 已登 findings **`R10-B4-01`（HTML-REV / important / open）**；
- 决策台账「本批落地自证」节同步登记；
- 下游 settlement PRD 就地写明**以 ENUM 口径为准、不据 BSM 摘要反向定义**，并标注该差异待用户批准后另批订正。

> **本节结论已失效（2026-08-06 补注，原文一字不删，仅作过程留痕）**：上文「该差异待用户批准后另批订正」当日即被**同批订正**——用户弹窗批准 **DEC-R8-66**，BSM 可分配池条镜像句已改齐、与 ENUM 口径逐字一致，findings `R10-B4-01` 转 `resolved`，下游 settlement PRD 的择一注记同批收敛为一致性说明（详见 **T7 补订正批**）。**本节不得再被引用为现存跨文件口径差。**

## 收工核对（第四批）

| 项 | 结果 |
|---|---|
| `node tools/verify-entry-anchors.mjs` | **PASS，EXIT=0**（注册表 1210、账本 917、承接矩阵 926、被检 Markdown 341、条目名引用 990） |
| `npm run verify:predeploy` | **EXIT=0**（全链门禁，含 entry-anchors、client-openapi、security-baseline、PII、测试库隔离等） |
| 三向自检 `~/r10b4_assert.py` | **PASS，EXIT=0**：①新话在位 36/36（HTML/spec）＋ 34/34（PRD）；②旧话残留 0；③33 落点覆盖 33/33；「明确不动的位置」6 处抽查全部保留；落点 5 百分比合计句 **1 份**（未留两份） |
| `npm run verify:html-ledger-sync` | EXIT=0，但输出「本次未改动业务最终解释权 HTML」——**空转假绿**（该门禁比对 `origin/main...HEAD`，本批未 commit，diff 为空）。**等效预跑**：工作区 diff 里三份 HTML 全部命中、`business-decision-ledger.md` 同批命中 → commit 后真跑应 PASS，**但此为预跑推断、非真跑结论** |
| `npm run verify:html-param-tier` | EXIT=0，同样**空转**。**等效预跑**：命中 1 处未标层级的新增行——BSM 请假扣薪条（因整行重写，行内含「1 月 31 日 22:00」等**举例日期**被数值正则命中）。该门禁**仅警告不阻断（源码末尾 `process.exit(0)`）**，且属其注释中明示的「法条编号／分类计数类误报」情形 |
| 三份 HTML 新 sha256 前 8 位 | ENUM `94da2f02`／BSM `3f51f8df`／PGM `5e5892c4`；**行数 256 / 1881 / 889 未变** |
| 行尾自检 | 全部改动文件与 HEAD 版逐份比对 CRLF/LF 形态：**变化 0 份**（三 HTML 与 spec 纯 LF；prd-audit 三份 CSV 纯 CRLF；决策台账维持 CRLF ＋ 76 行 LF 的既有混合形态） |
| `git diff --shortstat` | **82 files changed, 717 insertions(+), 402 deletions(-)**；本批 18 份文件合计 259+/97−，**无整文件级虚胖** |
| `git status --porcelain` | 83 项：82 份 `docs/` 与 `tools/verify-entry-anchors.mjs`（均为 C-R10 主批已有改动或本批范围内），未跟踪仅 `docs/prd-audit/rounds/r10/`；**无范围外文件** |
| commit / push / PR | **本批未执行**，按任务书留待 D 技术审查与 Fable 终验通过后的收尾批统一处理 |

## T7 补订正批（2026-08-06，用户弹窗批准同批订正 · DEC-R8-66）

来源：本批实施报告的遗留 1（矛盾4 只批 ENUM、BSM 镜像句未同步）与遗留 3（ENUM 内「本文件付账日归属规则」回指落空）。用户当日弹窗**批准同批订正**，故在本 worktree 继续叠加，纪律不变（只叠加、字节级替换保行尾、不 commit）。

| # | 文件 | 命中断言 | 改前 → 改后 |
|---|---|---|---|
| 订正1 | BSM 可分配池池金额条 | **原文命中 1 次（需 1）** | 「必须先按该票种的**可分配池金额**试算，确认第一段应分合计不会超过**池金额**」→「必须先按该票种的**最低可能可分配池金额**试算（**细则见枚举速查表同条**），确认第一段应分合计不会超过**该最低池金额**」 |
| 订正2 | ENUM 营业额抽成模式条 | **原文命中 1 次（需 1）** | 「仍由财务人员按**本文件**付账日归属规则指定」→「仍由财务人员按**业务系统地图的**付账日归属规则指定」 |

**BSM 落点 26 的「本文件」不动**——付账日归属规则本就写在业务系统地图内，该处回指成立。

配套同步：
- 账本 `source_text` 重取 **2 行 / 2 字段**：`BSM-0544`（可分配池池金额条）、`BEQ-0094`（营业额抽成模式条），按与 HTML 完全相同的逐字替换、按 `html_file` 限定；
- 注册表指纹**逐条实检后需刷新 0 条**（两处改动均不落在任一条目的前 24 字内，非推断，为全表重算比对结果）；
- `settlement-rebuild-prd-v1.0.md` 中本批写下的「跨文件口径差登记・以 ENUM 口径为准、不据 BSM 摘要反向定义」注记**按新事实收敛为一致性说明**（该注记内嵌了 BSM 旧句原文，不改即为旧话残留）；
- findings `R10-B4-01` → **`resolved`**，处置注明「2026-08-06 用户弹窗批准同批订正（DEC-R8-66）」；
- 决策台账**纯追加** `DEC-R8-66` 一节（记两项订正、注明系实施中发现并当场获批），并把本批落地自证节里「待用户批准后另批订正」的过时表述同步收敛。

**一次自检误报的澄清（留档，防复审重报）**：补订正脚本内附的账本全表复检报「不符 76 行」，经排查为**自检脚本自身的漏洞**——门禁检查⑤ 对 `entry_name = ORPHANED` 的行是**先 `continue` 跳过**的（R8 保留的口径变更史，现行 HTML 已无原文），自检脚本漏了这一跳；实测账本 ORPHANED 行数 **恰为 76**，与报警数逐一对应。真门禁 `verify-entry-anchors` 复跑 **PASS / EXIT=0**，账本无实际不一致。

### 补订正批收工核对

| 项 | 结果 |
|---|---|
| `node tools/verify-entry-anchors.mjs` | **PASS，EXIT=0**（注册表 1210、账本 917、承接矩阵 926、条目名引用 989） |
| `npm run verify:predeploy` | **EXIT=0** |
| 三向自检 `~/r10b4_assert.py` | **PASS，EXIT=0**（新话 36/36 ＋ 34/34、旧话残留 0、落点 33/33、落点 5 百分比合计句仍为 1 份、「明确不动的位置」6 处仍保留）。**自检脚本已同步 rebase**：两处补订正覆盖了已落地文本，期望值先套用同一订正再比对，否则拿旧期望比新现文会报假红（首跑即报了这 2 条假红，已修） |
| 三份 HTML sha256 前 8 位 | ENUM `10758753`／BSM `0a66315a`／PGM `5e5892c4`（PGM 未再变）；**行数 256 / 1881 / 889 仍未变** |
| 行尾自检（本次触碰文件） | BSM／ENUM／`settlement-rebuild-prd-v1.0.md` 纯 LF 不变；`authority-rule-ledger.csv`／`findings.csv` 纯 CRLF 不变；`business-decision-ledger.md` 维持 CRLF ＋ 76 行 LF 的既有混合形态；**形态变化 0 份** |
| commit / push / PR | **仍未执行** |

## T8 返工批（2026-08-06，D 轮独立技术审查 `TECHNICAL_VERDICT: NEEDS_FIX` 的修复）

来源：独立技术审查裁定 NEEDS_FIX，两项待修 ＋ 两项 Advisory 台账登记。纪律不变（只叠加、字节级替换保行尾、每处替换先断言原文唯一命中、不 commit / 不 push、不动三份 HTML、不动账本 `source_text` 列）。执行脚本 `~/r10-scripts/r10fix.py`（md5 `dc01611f`），5 处替换全部**原文命中 1 次（需 1）**、且新文预先断言不存在。

### R10-FIX-01（Blocking）试算基准两处 PRD 未同步

| # | 文件 | 命中断言 | 改前 → 改后 |
|---|---|---|---|
| 修1 | `docs/finance-settlement-product-prd-v0.1.md` L103 | **原文命中 1 次（需 1）** | 「必须先按该票种的**可分配池金额**试算，确认第一段应分合计不会超过**池金额**」→「必须先按该票种**在当前各项配置下的最低可能可分配池金额**试算（**细则见枚举速查表同条**），确认第一段应分合计不会超过**该最低池金额**」；同时在该 bullet 标题内补依据标注「（2026-08-06 R10 返工同步，依据 ENUM／BSM「酒吧通票可分配池」条，DEC-R8-48/66）」 |
| 修2 | `docs/settlement-rebuild-prd-v1.0.md` L102 | **原文命中 1 次（需 1）** | 依据括注内指向 BSM 的引文「必须先按该票种的**可分配池金额**试算」（BSM 已无此字符串、且与本文件 L104「两份 HTML 已一致」自相矛盾）→ 改为 BSM 现文逐字「必须先按该票种的**最低可能可分配池金额**试算」，L102 与 L104 互相打脸就此消除 |

**引文与 HTML 现文的逐字核对（实跑，非推断）**：BSM 含「第一段的固定金额或固定单价在配置生效之前，必须先按该票种的最低可能可分配池金额试算」**1 次**；ENUM 含「必须先按该票种在当前各项配置下的最低可能可分配池金额试算」**1 次**；「确认第一段应分合计不会超过该最低池金额」ENUM **1 次**、BSM **1 次**。修1 抄 ENUM 口径（下游为 finance PRD，ENUM 是细则出处）、修2 抄 BSM 现文（该处本就是 BSM 引文）。

### R10-FIX-02（Important）spec `ignored` 残留

| # | 文件 | 命中断言 | 改前 → 改后 |
|---|---|---|---|
| 修3 | `docs/third-party-access-layer-spec-v0.2.md` L88 | **原文命中 1 次（需 1）** | `差异记录：open → pending_review → closed / ignored` → `差异记录：open → pending_review → closed`（直接删「/ ignored」，与 L394 已裁的「差异记录四态、原第五态『已忽略』已删除，依据 DEC-R4-2026-08-04-43」一致） |

**全文件 `ignored` 复扫（改后实测）**：命中 **3 处，全部落在已标停用的位置**——L391 回流记录（`abnormal（`ignored` 停用）`）、L393 接入异常（`closed（`ignored` 停用）`）、L394 差异记录（`closed（`ignored` 停用）`）。**无未标注残留。**

### 附带小项（纯台账，不碰正文口径）

| # | 文件 | 命中断言 | 内容 |
|---|---|---|---|
| 修4 | 本台账 **T6 节** | **原文命中 1 次（需 1）** | **原文一字不删**，在节末追加失效标注：T6 写的「该差异待用户批准后另批订正」当日即被同批订正（DEC-R8-66），BSM 镜像句已改齐、findings `R10-B4-01` 转 `resolved`，本节仅作过程留痕、**不得再被引用为现存跨文件口径差**（详见 T7） |
| 修5 | `docs/prd-audit/rounds/r8/findings.csv` | 追加 1570 字节、**2 行 CRLF** | 追加 `R10-D-01`（RESIDUAL / advisory / open，审查 A-1）与 `R10-D-02`（GAP / advisory / open，审查 A-2）；编号接续现有 R10 序列（O / U / B4 之后新起 D 子系＝D 轮技术审查），列数 8 与表头一致、`type`／`severity`／`status` 取值全部沿用文件内既有词表 |

- **R10-D-01（审查 A-1，业务感知提示，条文本身无错）**：出厂默认参数下（押金＝三个月月租 R、清退欠缴阈值＝两个月、月均应缴费用 T 含房租＋物业费＋水电能源费故 T≥R），押金硬线的触发欠费额 `3R−T` 恒不高于欠缴月数条的 `2T`（`3R−T≤2T ⟺ R≤T`，恒成立），故 BSM 明写的「两条触发条件取严、先到先触发」在出厂默认下**实际长期只有押金硬线单条生效**。候选处置＝代码轮在后台参数说明处提示该耦合，或由用户另批调整出厂默认。**不改 HTML、不构成缺陷、不阻断本轮。**
- **R10-D-02（审查 A-2，缺项）**：欠缴月数折算「当期欠费总额 ÷ 该商户当前月均应缴费用」在除数为 0 时（新签约尚未出账、账单未生成）**无兜底口径，三份 HTML 均未写**。按用户 2026-08-06 裁决，本轮只登缺口、**不改 HTML**——补兜底句属下一批 C 类草案候选，须另出草案经用户逐句批准。

### 返工批收工核对

| 项 | 结果 |
|---|---|
| 全库复扫「按该票种的可分配池金额试算」 | 现行正文 **归零**；残 10 处**全部落在历史留痕排除口径内**：`prd-audit-fix-change-log-20260804.md`（`change-log`）、`rounds/r7/anchor-rebuild-a1.report.md` ×2（`anchor-rebuild`／`.report.`／`/rounds/`）、`rounds/r7/html-map-bsm-1.md`（`html-map`）、`rounds/r9/ledger-source-refresh.csv` ×2（`/rounds/`）、`rounds/r8/findings.csv`（R10-B4-01 改前证据，已 `resolved`）、`rounds/r10/worklog.md`（T6 过程留痕，已补失效标注）、`business-decision-ledger.md` ×2（DEC-R8-66 裁决原文引录＋第四批自证节，均已自带「已同批订正」结论） |
| 全库复扫「不会超过池金额」 | 现行正文 **归零**；残 5 处全部在排除口径内（`rounds/r8/findings.csv`、`rounds/r7/html-map-pgm-enum.md`、`rounds/r9/ledger-source-refresh.csv` ×2、`business-decision-ledger.md`） |
| 「最低可能可分配池金额」在位 | `finance-settlement-product-prd-v0.1.md` **1 处**、`settlement-rebuild-prd-v1.0.md` **3 处**（原 2 处 ＋ 修2 新增 1 处） |
| spec 全文件 `ignored` 复扫 | **3 处全部落在已标「`ignored` 停用」的位置**，无未标注残留 |
| `node tools/verify-entry-anchors.mjs` | **PASS，EXIT=0**（注册表 1210、账本 917、承接矩阵 926、被检 Markdown 341、条目名引用 990） |
| `npm run verify:predeploy` | **EXIT=0**（全链门禁，含 entry-anchors、client-openapi、client-payload、security-baseline、prod-docs-access、production-readiness、production-blockers、PII 加密、测试库隔离） |
| 行尾／BOM 自检（本次触碰 5 份） | 逐份与 HEAD 比对**形态零变化**：finance PRD `pure-LF`→`pure-LF`；settlement-rebuild `pure-LF`→`pure-LF`；spec `pure-LF+BOM`→`pure-LF+BOM`；`findings.csv` `pure-CRLF+BOM`→`pure-CRLF+BOM`；`worklog.md` 本轮新增文件、`pure-LF`（无 HEAD 版可比）。**无孤立 CR、无 BOM 增删。** |
| `git diff --shortstat` | 725→**729 insertions**、402→**404 deletions**，**82 files changed 未变**。逐项对账：findings.csv ＋2 ins；spec ＋1/−1；finance PRD ＋1/−1；settlement-rebuild ±0（L102 在返工前即已是本轮 diff 的新增行，二次改写不新增计数）；worklog 未跟踪不计入 —— 合计 ＋4 ins／＋2 del，**与实测完全一致，无隐藏改动** |
| `git status --porcelain` | **83 项未变**：82 份 `docs/` ＋ `tools/verify-entry-anchors.mjs`（后者为 R10 主批既有改动，本返工批未触碰），未跟踪仅 `docs/prd-audit/rounds/r10/`。**无范围外文件** |
| 三份 HTML | **本返工批一字未改**（返工后实测 sha256 与 T7 收工记录逐字一致：ENUM `10758753`／BSM `0a66315a`／PGM `5e5892c4`；行数 256／1881／889 亦未变） |
| 账本 `source_text` 列 | **本返工批未动**（HTML 未改，无需重取） |
| commit / push / PR | **仍未执行** |

## T9 正式偏差声明（FIX-03，2026-08-06 用户弹窗裁定：留下、不回退）

**裁决原文**：R10 第四批的裁决措辞为「**端点标 deprecated 归代码轮**」。

**本轮实施实际做了什么**：实施时已在 `docs/openapi/openapi-p0.yaml` 与 `docs/openapi/openapi-p0.json` 对下列三个操作标了 `deprecated: true`，**超出该次裁决的字面范围**——

| # | 方法 | 路径 | operationId | summary |
|---|---|---|---|---|
| 1 | POST | `/api/v1/amusement/manual-overrides` | `createAmusementManualOverride` | 游乐人工放行（已废止，不得新增调用） |
| 2 | GET | `/api/v1/amusement/manual-overrides` | `listAmusementManualOverrides` | 查询游乐人工放行（已废止，不得新增调用） |
| 3 | POST | `/api/v1/ticketing/verifications/preview` | `previewTicketVerification` | 票务核销预览（已废止，不得新增调用） |

（实测：两份 openapi 各 5 处 `deprecated: true`，其中 **3 处为本轮 diff 新增**、即上表三项；第 4 处 `POST /api/v1/settlement/config/attribution-rules/{id}/activate`（弃用薄别名）为改前既有，与本轮无关。）

**用户裁定（2026-08-06 弹窗）**：三处 `deprecated: true` **留下、不回退**。理由：**文档层的废止提示零破坏性、早标早止损**。

**仍归代码轮的部分（本轮不做）**：端点的**真实下线**——路由摘除、调用方迁移、返回 410／404、以及相应测试与客户端改造。本条只解决「文档标注」，**不构成端点已下线的结论**。

**登记去向**：决策台账纯追加 `DEC-R8-67`；`findings.csv` 中 `deprecated` / `manual-overrides` / `verifications/preview` / `FIX-03` **零命中，无对应行可同步，按指示不新增**。

### FIX-03 收工核对

| 项 | 结果 |
|---|---|
| `node tools/verify-entry-anchors.mjs` | **PASS，EXIT=0**（追加后真跑；注册表 1210、账本 917、承接矩阵 926、被检 Markdown 341、条目名引用 990） |
| `npm run verify:predeploy` | **EXIT=0**（追加后真跑，全链门禁绿） |
| 本次触碰文件 | `docs/prd-audit/rounds/r10/worklog.md`（本节）、`docs/prd-audit/business-decision-ledger.md`（DEC-R8-67 纯追加）。**openapi 两份本次未再触碰**（维持第四批已标状态） |
| 三份 HTML／账本 `source_text` | **未动** |
| commit / push / PR | **仍未执行** |
