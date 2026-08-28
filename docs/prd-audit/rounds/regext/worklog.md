# 账本扩登记轮（regext）工作日志

> 背景：R8-U-02「HTML 内容无账本落点」。R10 已按 DEC-R8-65 先登 U-1~U-12 优先清单，
> 并裁定「105+ 全量清单下轮出」。本轮＝全量扩登记，按 10 条一批起草、串行落盘。
> 本文件只记过程与证据，规则正文一律以三份 HTML 现文为准。

---

## 一、备料步骤（2026-08-06）

### 1.1 工作区与基线

工作区：`~/wt-regext`，分支 `docs/rule-registry-expansion`，起点 `origin/main = c7d3feeb`。

```
$ cd ~/complex-management && git fetch origin && git rev-parse origin/main
c7d3feebe12bf7d80cc6ff1b3e13624585ca13b1

$ git worktree add ~/wt-regext -b docs/rule-registry-expansion origin/main
Preparing worktree (new branch 'docs/rule-registry-expansion')
branch 'docs/rule-registry-expansion' set up to track 'origin/main'.
HEAD is now at c7d3feeb Merge pull request #342 from jxh1979/docs/r10-verification-fixes
```

三份最终解释权 HTML 基线哈希（与 R10 收官记载逐位一致）：

```
$ sha256sum docs/business-enum-quick-reference-v0.1.html \
            docs/business-system-map-for-operations-v0.1.html \
            docs/platform-governance-map-for-operations-v0.1.html
10758753fea57bb1cda2d041960d218acd07e8855829d307514597ab1921aa4c  docs/business-enum-quick-reference-v0.1.html
0a66315af52a98a256181e226e1b42eb9f715896b0c4354da2dec8755d224906  docs/business-system-map-for-operations-v0.1.html
5e5892c43d4ee48b40cf06d92a68ab396ecd9bfb555ac4984f054c285476db69  docs/platform-governance-map-for-operations-v0.1.html
```

条目锚点门禁（备料前，基线态）：

```
$ node tools/verify-entry-anchors.mjs ; echo EXIT=$?
[entry-anchors] 注册表 1210 行；账本 917 行；承接矩阵 926 行（同名 917、账本中无此 rule_id 9）；
被检 Markdown 341 份；条目名引用 990 处（注册表原名 885 处，正文唯一定位 105 处）。
[entry-anchors] PASS：条目名唯一、引用可唯一定位、正文无行号锚点、账本已登记条目正文与 HTML 现文一致
（未登记条目不在保护范围，见 R8-U-02）、账本与承接矩阵一一对应。
EXIT=0
```

### 1.2 差集口径（机械求差，不靠人读）

- 数据源：`docs/prd-audit/entry-registry.csv`（1210 行 ＝ entry 1059 ＋ section 151）、
  `docs/prd-audit/authority-rule-ledger.csv`（917 数据行）。
- 判定：注册表中 `entry_kind == "entry"` 的条目，若其 `(html_file, entry_name)` 二元组
  未出现在账本任何一行的 `(html_file, entry_name)` 中，即判为**未登记**。
  - 用二元组而非单看条目名：条目名只在同一份 HTML 内保证唯一（门禁检查①的口径）。
  - 账本中 `entry_name == "ORPHANED"` 的 76 行是 R8 保留的口径变更史（现行 HTML 已无原文），
    这类行不会匹配上任何注册表条目，自然不影响差集。
- CSV 一律用 Python `csv` 模块解析（引号内含逗号的单元格在这两份表里普遍存在），未手写 split。

### 1.3 正文提取口径（与门禁同源，先反验后使用）

提取算法与门禁 `tools/verify-entry-anchors.mjs` 的 `flatEntry()` 同源：
取 `line_now` 指向的那一行；若该行是未闭合的表格行（`<tr` 且无 `</tr>`）则顺延至 `</tr>`；
去注释、去标签、解 HTML 实体、压缩空白（中文字符相邻的空格删除，英文词间空格保留）。

**反验（关键，防止口径自证）**：用 841 条**已登记**条目（917 行账本 − 76 行 ORPHANED）跑同一算法，
把提取结果与账本现有 `source_text` 逐字比对：

```
口径验证：完全一致=831  账本是行文本子串=10  对不上=0  账本名不在注册表=0
```

10 条「子串」是 R10 按 §K.1 规则级登记的行（`source_text` 取条目内的一句原文），属已知设计，非偏差。
0 条对不上 ⇒ 本备料产出的 `source_text` 直接落账本即可满足门禁检查⑤
（`norm(source_text)` 须逐字存在于该 HTML 去空白全文中）。

### 1.4 差集结果

```
$ python3 ~/regext_make_shards.py
未登记条目总数 = 226
注册表 1210 行（entry 1059 / section 151）；账本 917 行；已登记条目 833
分布: {'BSM': 144, 'PGM': 79, 'BEQ': 3}
片数 = 10，最大片 = 27
自检：分片总条数 226 == 差集 226，entry_id 无重复。正文未含条目名首8字的条数 = 0
```

自检三条断言：分片总数守恒（226 == 226）、`entry_id` 全局不重复、每条正文都含其条目名
（重名条目在注册表里带「章节·」消歧前缀，比对时取 `·` 末段）。

### 1.5 226 与 R9 记载 233 的对账

R9 worklog 记「注册表口径下实测为 233 条条目无账本行」。用**同一份脚本**在 R9 收官提交
`c28215b6` 上回跑，精确复现 233，证明本轮口径与 R9 记载口径同源：

```
c28215b6    注册表=1210 entry=1059 账本=907 未登记entry=233 {BSM:150, PGM:83}
origin/main 注册表=1210 entry=1059 账本=917 未登记entry=226 {BSM:147, PGM:79}
```

（上表 BSM/PGM 计数按 html_file 前缀统计，含 ENUM 0/3 未列。）

7 条之差全部来自 R10，逐条可解释：

| 变化 | 条数 | 说明 |
|---|---|---|
| R10 新登且原先零账本行的条目 | −5 | `PGM-0194` 留痕开关条、`PGM-0195` 数据隔离条、`PGM-0197` 娱乐平台公司条、`BSM-0547` 会员规则计算条、`BSM-0548~0550` 三个付账日条（3 行同一条目） |
| R10 把两行 ORPHANED 脱孤、恢复真实条目名 | −2 | `BSM-0394`（ORPHANED → 演出票）、`PGM-0106`（ORPHANED → 触摸屏一体机） |
| R10 改 HTML 致条目名变短，旧名移出、新名进入 | −1 ＋1 | 「管理游乐项目、开放时间、场次、容量、核销、巡检」→「管理游乐项目、开放时间」，该条目仍未登记 |
| 合计 | 233 − 8 ＋ 1 = **226** | |

另有 3 条 R10 账本行（`PGM-0196`、`BSM-0551`、`BEQ-0169`）落在**已登记**条目上（规则级补登），
不改变差集，故 R10 十行只让差集减少 5。

### 1.6 分片产出

`docs/prd-audit/rounds/regext/shard-NN.jsonl`，每行一个条目，UTF-8、LF、`ensure_ascii=false`。
字段：`entry_id / code / html_file / entry_name / line_hint / section_h2 / section_h3 / source_text`。
分片规则：不跨 HTML 混片，片内按文档顺序（`line_now` 升序），每片 ≤28 条，同一 HTML 内均分片长
（避免尾片只剩个位数）。

| 片号 | HTML | 条数 | 首条 | 末条 |
|---|---|---|---|---|
| shard-01 | BSM | 24 | `BSM-E0001` 给业务人员看的系统说明 | `BSM-E0029` 场内核销/点酒 |
| shard-02 | BSM | 24 | `BSM-E0030` 进场之后，工作手机扫客人二维码核销各酒吧入场、赠饮 | `BSM-E0070` 游乐管理业务 |
| shard-03 | BSM | 24 | `BSM-E0071` 管理游乐项目、开放时间 | `BSM-E0099` 娱乐平台公司负责酒吧经营域、酒吧通票 |
| shard-04 | BSM | 24 | `BSM-E0120` 票务系统只提交订单、支付、履约、退款、冲正和核销事实 | `BSM-E0190` 账户注销 |
| shard-05 | BSM | 24 | `BSM-E0194` 我的存酒 | `BSM-E0543` 影响结算的规则由各自责任主体在系统中配置、审批和版本化 |
| shard-06 | BSM | 24 | `BSM-E0548` 按营业日生成日结批次。全系统的钱账统一按「营业日」归集 | `BSM-E0709` 游乐容量只按单个项目的实时在玩人数控制，排队不占容量 |
| shard-07 | PGM | 27 | `PGM-E0001` 平台治理层 · 给业务人员看的说明 | `PGM-E0034` 再看合同 |
| shard-08 | PGM | 26 | `PGM-E0035` 合同是否生效，是否允许这个业务、位置和结算口径 | `PGM-E0061` 管理工作手机、平板、一体机和扫码设备的台账、领还 |
| shard-09 | PGM | 26 | `PGM-E0062` 商户侧边界 | `PGM-E0277` 免单、退款、调账 |
| shard-10 | BEQ | 3 | `ENUM-E0001` 速查表页眉 | `ENUM-E0167` 解释权声明 |

合计 226 条。

### 1.7 交接给起草阶段的两点事实

1. **226 是全量未登记条目，不是 226 条规则。** 用户裁的「约 105 条规则性条目」是其中的子集：
   正文长度分布为 `<20 字 70 条 / 20–59 字 59 条 / 60–149 字 64 条 / ≥150 字 33 条`，
   其中大量短条目是页眉、导语、目录项、流程图步骤编号（例如 `BSM-E0016` 条目名与正文都只有一个「1」，
   注册表把流程图步骤号也登记成了条目）。起草代理须先判 **规则性 / 非规则性**，
   非规则条目只登结论不进账本，判定理由须落文，不得静默丢弃。
2. **本备料未改 HTML、未改账本/承接矩阵/findings、未 commit。** 分片是只读派工单；
   落盘阶段仍须逐条回到 HTML 现文核对，`source_text` 以本文件 1.3 口径为准可直接满足门禁检查⑤，
   但 `line_hint` 只是提示，不得当锚点用（行号锚点制度已于 R9 废除）。

### 1.8 备料阶段实际执行的命令

```
ssh james@192.168.88.160
cd ~/complex-management && git fetch origin && git rev-parse origin/main
git worktree add ~/wt-regext -b docs/rule-registry-expansion origin/main
cd ~/wt-regext && sha256sum docs/business-enum-quick-reference-v0.1.html \
    docs/business-system-map-for-operations-v0.1.html docs/platform-governance-map-for-operations-v0.1.html
node tools/verify-entry-anchors.mjs            # EXIT=0
python3 ~/probe.py                             # 口径反验：831 全等 / 10 子串 / 0 对不上
python3 ~/probe2.py                            # R10 十行覆盖 8 个条目、其中 5 个原先零账本行
python3 ~/probe3.py                            # 在 c28215b6 上回跑复现 233
python3 ~/regext_make_shards.py                # 产出 10 片 226 条 ＋ 三条自检断言
node tools/verify-entry-anchors.mjs            # 备料后复跑，EXIT=0（新增文件不影响门禁）
git status --short                             # 只有 docs/prd-audit/rounds/regext/ 为未跟踪新增
```


---

## 二、合并落盘（2026-08-06，单一代理串行写表）

### 2.1 落盘前独立复验（门禁检查⑤同源算法）

10 片草稿共 226 条目 / 305 条账本草稿行，**逐行独立复验**：用与 `tools/verify-entry-anchors.mjs`
完全同源的 `plainText()` + `norm()`（去注释/去标签/解实体/压空白，剥「」『』“”"'‘’ 引号）把每行
`source_text` 与对应 HTML 现文做子串匹配。

```
条目 226（rule 132 / non-rule 94），账本草稿行 305
门禁5同源复验：不中 0 行
检查4条目名（账本 entry_name 必须在注册表内同 HTML 存在）：不合格 0 行
账本 18 列缺失：0；placeholder 重复：0；decision_status 全 AUTHORITATIVE
```

**剔除行：无**（305 行全部存活，未改写任何一个字去硬凑）。

### 2.2 裁决③：治理顺序五步跨片规则——主路径成立，未走退路

章节锚**小样先验证**后再用：注册表 `PGM-E0029` 为 `entry_kind=section`、`entry_name=治理顺序`；
门禁检查④的 `nameSeen` 表由注册表**全部行**（entry ＋ section）构建，故章节名可作账本 `entry_name`。
整段五步正文命中实测：

```
整段（含 h2 标题）命中: True
不含标题命中: True
```

因此按主路径补登 **1 行整条顺序规则**（`entry_name=治理顺序`，占位 `S07-R08`，插在 shard-07 末尾），
**未启用退路**（未在 S08-R01 加 notes、未另加 advisory 提示行；该行自身的承接缺口另立 `REGX-16`）。
账本新增行数因此为 305 ＋ 1 ＝ **306**。

### 2.3 裁决⑨：MISSING／升格 finding 的「零命中」放宽复验

放宽口径＝去全部空白 ＋ 全半角标点互换（含全角数字/字母）＋ 剥引号后做子串匹配；
检索面 `docs/**/*.md` 302 份（剔除 `prd-audit/`、`_archive`、`node_modules`）。

| 断言归属 | 复验短语 | 放宽后命中 | 结论 |
|---|---|---|---|
| S01-R04 | 综合体官方小程序 ／ 单店门票允许客人 | 0 ／ 0 | 零命中成立 |
| S02-R04 | 受理部门 ／ 任何一个员工都可以发起工单 | 1 ／ 1（均为 `prd-resync-change-log-20260805.md`，HTML 变更记录非承接） | 零承接成立 |
| S02-R05 | 转指定其他部门协助 | 1（同上变更日志） | 零承接成立 |
| S04-R50 | 不能拿下单时间当支付时间用 | 3（bar／service-order／member-loyalty 三域，**票务域 0**） | 票务域零承接成立 |
| S05-R30 | 不得迟于次月15日 ／ 次月15日 | 0 ／ 2（决策日志与 superpowers plan 的**积分商户结算付款日**，非薪资发放日） | 零命中成立 |
| S05-R34 | 订单最终保留收入 ／ 反向结算记录 | 0 ／ 2（票务分账逐段冲回，非提成/服务费） | 零命中成立 |
| S05-R60 | 餐饮与零售商户 ／ 商户档案上指定的主管公司 | 0 ／ 0 | 零命中成立 |
| S05-R67 | 当月内完全清理 ／ 平台无法控制 | 0 ／ 0 | 零命中成立 |
| S05-R68 | 账期结束后一个月以内完成 ／ 一个月以内完成 | 0 ／ 0 | 零命中成立 |
| S07-R02 | 名称、编号、金额、状态、人员 ／ 见名可点 | 0 ／ 0 | 零命中成立 |
| S06-R12（升格） | 该服务发生地所属的酒吧商户 ／ 服务发生地 | 0 ／ 0 | 零命中成立 |
| S06-R13（升格） | 计入其净利润基数 ／ 净利润基数 | 0 ／ 4（bar PRD §10.3.1 服务费扣留部分与变更日志，非服务点单管理费） | 零命中成立 |
| S09-R07（升格） | 不管理娱乐平台公司 ／ 不管理酒吧 | 0 ／ 0 | 零命中成立 |
| S09-R20/21（升格） | 统一审批模型 | 6（回读无一为适用任意单据的通用条款） | 场景化承接判定成立 |
| S07-R08（新增） | 先看对象 ／ 最后留痕 | 0 ／ 0 | 顺序本体零承接成立 |

**翻案：无。**放宽口径下没有任何一条 MISSING／升格断言被推翻，全部维持原判。

### 2.4 逐批落盘与门禁

步骤 0（先于第 1 批）：按裁决④对既有 findings 行 `R10-U-03` 的 `disposition` 列**就地加注**
（原文命中唯一、不删不移、其余列一字未动），门禁 `EXIT=0`。

每批 = 10 行账本 ＋ 同批 10 行承接矩阵（检查⑥一一对应）＋ 该批涉及的 findings 行；
落盘后真跑 `node tools/verify-entry-anchors.mjs`，`EXIT=0` 才进下一批。
落盘前对三份台账做了字节级备份（`~/regext-backup/*.orig`），任一批红即整批回滚。

| 批次 | 账本 rule_id 区间 | 账本行 | 承接行 | findings 行 | 门禁 EXIT |
|---|---|---|---|---|---|
| 批 01 | `BSM-0552` ～ `BSM-0561` | 10 | 10 | 1 | 0 |
| 批 02 | `BSM-0562` ～ `BSM-0571` | 10 | 10 | 2 | 0 |
| 批 03 | `BSM-0572` ～ `BSM-0581` | 10 | 10 | 0 | 0 |
| 批 04 | `BSM-0582` ～ `BSM-0591` | 10 | 10 | 0 | 0 |
| 批 05 | `BSM-0592` ～ `BSM-0601` | 10 | 10 | 0 | 0 |
| 批 06 | `BSM-0602` ～ `BSM-0611` | 10 | 10 | 0 | 0 |
| 批 07 | `BSM-0612` ～ `BSM-0621` | 10 | 10 | 0 | 0 |
| 批 08 | `BSM-0622` ～ `BSM-0631` | 10 | 10 | 0 | 0 |
| 批 09 | `BSM-0632` ～ `BSM-0641` | 10 | 10 | 0 | 0 |
| 批 10 | `BSM-0642` ～ `BSM-0651` | 10 | 10 | 2 | 0 |
| 批 11 | `BSM-0652` ～ `BSM-0661` | 10 | 10 | 1 | 0 |
| 批 12 | `BSM-0662` ～ `BSM-0671` | 10 | 10 | 0 | 0 |
| 批 13 | `BSM-0672` ～ `BSM-0681` | 10 | 10 | 0 | 0 |
| 批 14 | `BSM-0682` ～ `BSM-0691` | 10 | 10 | 0 | 0 |
| 批 15 | `BSM-0692` ～ `BSM-0701` | 10 | 10 | 1 | 0 |
| 批 16 | `BSM-0702` ～ `BSM-0711` | 10 | 10 | 2 | 0 |
| 批 17 | `BSM-0712` ～ `BSM-0721` | 10 | 10 | 0 | 0 |
| 批 18 | `BSM-0722` ～ `BSM-0731` | 10 | 10 | 0 | 0 |
| 批 19 | `BSM-0732` ～ `BSM-0741` | 10 | 10 | 2 | 0 |
| 批 20 | `BSM-0742` ～ `BSM-0751` | 10 | 10 | 1 | 0 |
| 批 21 | `BSM-0752` ～ `BSM-0761` | 10 | 10 | 1 | 0 |
| 批 22 | `BSM-0762` ～ `BSM-0771` | 10 | 10 | 1 | 0 |
| 批 23 | `BSM-0772` ～ `BSM-0781` | 10 | 10 | 0 | 0 |
| 批 24 | `BSM-0782` ～ `BSM-0791` | 10 | 10 | 0 | 0 |
| 批 25 | `BSM-0792` ～ `PGM-0205` | 10 | 10 | 2 | 0 |
| 批 26 | `PGM-0206` ～ `PGM-0215` | 10 | 10 | 0 | 0 |
| 批 27 | `PGM-0216` ～ `PGM-0225` | 10 | 10 | 1 | 0 |
| 批 28 | `PGM-0226` ～ `PGM-0235` | 10 | 10 | 1 | 0 |
| 批 29 | `PGM-0236` ～ `PGM-0245` | 10 | 10 | 0 | 0 |
| 批 30 | `PGM-0246` ～ `PGM-0255` | 10 | 10 | 0 | 0 |
| 批 31 | `PGM-0256` ～ `BEQ-0173` | 6 | 6 | 0 | 0 |

**31 批全绿，零回滚、零返工。**门禁计数逐批递增可自证：账本 917 → 1223 行，承接矩阵 926 → 1232 行。

### 2.5 占位 ID → 最终号映射

续现有最大号编号（`BSM-0551` / `PGM-0197` / `BEQ-0169` 之后），按分片顺序 01→10、片内保持草稿顺序：

| 分片 | 行数 | 占位区间 | 最终号区间 |
|---|---|---|---|
| shard-01 | 9 | `S01-R01` → `S01-R09` | `BSM-0552` → `BSM-0560` |
| shard-02 | 21 | `S02-R01` → `S02-R21` | `BSM-0561` → `BSM-0581` |
| shard-03 | 16 | `S03-R01` → `S03-R16` | `BSM-0582` → `BSM-0597` |
| shard-04 | 77 | `S04-R01` → `S04-R77` | `BSM-0598` → `BSM-0674` |
| shard-05 | 75 | `S05-R01` → `S05-R75` | `BSM-0675` → `BSM-0749` |
| shard-06 | 44 | `S06-R01` → `S06-R44` | `BSM-0750` → `BSM-0793` |
| shard-07 | 8 | `S07-R01` → `S07-R08` | `PGM-0198` → `PGM-0205` |
| shard-08 | 7 | `S08-R01` → `S08-R07` | `PGM-0206` → `PGM-0212` |
| shard-09 | 45 | `S09-R01` → `S09-R45` | `PGM-0213` → `PGM-0257` |
| shard-10 | 4 | `S10-R01` → `S10-R04` | `BEQ-0170` → `BEQ-0173` |

全量 306 条逐条映射：

```
S01-R01 -> BSM-0552
S01-R02 -> BSM-0553
S01-R03 -> BSM-0554
S01-R04 -> BSM-0555
S01-R05 -> BSM-0556
S01-R06 -> BSM-0557
S01-R07 -> BSM-0558
S01-R08 -> BSM-0559
S01-R09 -> BSM-0560
S02-R01 -> BSM-0561
S02-R02 -> BSM-0562
S02-R03 -> BSM-0563
S02-R04 -> BSM-0564
S02-R05 -> BSM-0565
S02-R06 -> BSM-0566
S02-R07 -> BSM-0567
S02-R08 -> BSM-0568
S02-R09 -> BSM-0569
S02-R10 -> BSM-0570
S02-R11 -> BSM-0571
S02-R12 -> BSM-0572
S02-R13 -> BSM-0573
S02-R14 -> BSM-0574
S02-R15 -> BSM-0575
S02-R16 -> BSM-0576
S02-R17 -> BSM-0577
S02-R18 -> BSM-0578
S02-R19 -> BSM-0579
S02-R20 -> BSM-0580
S02-R21 -> BSM-0581
S03-R01 -> BSM-0582
S03-R02 -> BSM-0583
S03-R03 -> BSM-0584
S03-R04 -> BSM-0585
S03-R05 -> BSM-0586
S03-R06 -> BSM-0587
S03-R07 -> BSM-0588
S03-R08 -> BSM-0589
S03-R09 -> BSM-0590
S03-R10 -> BSM-0591
S03-R11 -> BSM-0592
S03-R12 -> BSM-0593
S03-R13 -> BSM-0594
S03-R14 -> BSM-0595
S03-R15 -> BSM-0596
S03-R16 -> BSM-0597
S04-R01 -> BSM-0598
S04-R02 -> BSM-0599
S04-R03 -> BSM-0600
S04-R04 -> BSM-0601
S04-R05 -> BSM-0602
S04-R06 -> BSM-0603
S04-R07 -> BSM-0604
S04-R08 -> BSM-0605
S04-R09 -> BSM-0606
S04-R10 -> BSM-0607
S04-R11 -> BSM-0608
S04-R12 -> BSM-0609
S04-R13 -> BSM-0610
S04-R14 -> BSM-0611
S04-R15 -> BSM-0612
S04-R16 -> BSM-0613
S04-R17 -> BSM-0614
S04-R18 -> BSM-0615
S04-R19 -> BSM-0616
S04-R20 -> BSM-0617
S04-R21 -> BSM-0618
S04-R22 -> BSM-0619
S04-R23 -> BSM-0620
S04-R24 -> BSM-0621
S04-R25 -> BSM-0622
S04-R26 -> BSM-0623
S04-R27 -> BSM-0624
S04-R28 -> BSM-0625
S04-R29 -> BSM-0626
S04-R30 -> BSM-0627
S04-R31 -> BSM-0628
S04-R32 -> BSM-0629
S04-R33 -> BSM-0630
S04-R34 -> BSM-0631
S04-R35 -> BSM-0632
S04-R36 -> BSM-0633
S04-R37 -> BSM-0634
S04-R38 -> BSM-0635
S04-R39 -> BSM-0636
S04-R40 -> BSM-0637
S04-R41 -> BSM-0638
S04-R42 -> BSM-0639
S04-R43 -> BSM-0640
S04-R44 -> BSM-0641
S04-R45 -> BSM-0642
S04-R46 -> BSM-0643
S04-R47 -> BSM-0644
S04-R48 -> BSM-0645
S04-R49 -> BSM-0646
S04-R50 -> BSM-0647
S04-R51 -> BSM-0648
S04-R52 -> BSM-0649
S04-R53 -> BSM-0650
S04-R54 -> BSM-0651
S04-R55 -> BSM-0652
S04-R56 -> BSM-0653
S04-R57 -> BSM-0654
S04-R58 -> BSM-0655
S04-R59 -> BSM-0656
S04-R60 -> BSM-0657
S04-R61 -> BSM-0658
S04-R62 -> BSM-0659
S04-R63 -> BSM-0660
S04-R64 -> BSM-0661
S04-R65 -> BSM-0662
S04-R66 -> BSM-0663
S04-R67 -> BSM-0664
S04-R68 -> BSM-0665
S04-R69 -> BSM-0666
S04-R70 -> BSM-0667
S04-R71 -> BSM-0668
S04-R72 -> BSM-0669
S04-R73 -> BSM-0670
S04-R74 -> BSM-0671
S04-R75 -> BSM-0672
S04-R76 -> BSM-0673
S04-R77 -> BSM-0674
S05-R01 -> BSM-0675
S05-R02 -> BSM-0676
S05-R03 -> BSM-0677
S05-R04 -> BSM-0678
S05-R05 -> BSM-0679
S05-R06 -> BSM-0680
S05-R07 -> BSM-0681
S05-R08 -> BSM-0682
S05-R09 -> BSM-0683
S05-R10 -> BSM-0684
S05-R11 -> BSM-0685
S05-R12 -> BSM-0686
S05-R13 -> BSM-0687
S05-R14 -> BSM-0688
S05-R15 -> BSM-0689
S05-R16 -> BSM-0690
S05-R17 -> BSM-0691
S05-R18 -> BSM-0692
S05-R19 -> BSM-0693
S05-R20 -> BSM-0694
S05-R21 -> BSM-0695
S05-R22 -> BSM-0696
S05-R23 -> BSM-0697
S05-R24 -> BSM-0698
S05-R25 -> BSM-0699
S05-R26 -> BSM-0700
S05-R27 -> BSM-0701
S05-R28 -> BSM-0702
S05-R29 -> BSM-0703
S05-R30 -> BSM-0704
S05-R31 -> BSM-0705
S05-R32 -> BSM-0706
S05-R33 -> BSM-0707
S05-R34 -> BSM-0708
S05-R35 -> BSM-0709
S05-R36 -> BSM-0710
S05-R37 -> BSM-0711
S05-R38 -> BSM-0712
S05-R39 -> BSM-0713
S05-R40 -> BSM-0714
S05-R41 -> BSM-0715
S05-R42 -> BSM-0716
S05-R43 -> BSM-0717
S05-R44 -> BSM-0718
S05-R45 -> BSM-0719
S05-R46 -> BSM-0720
S05-R47 -> BSM-0721
S05-R48 -> BSM-0722
S05-R49 -> BSM-0723
S05-R50 -> BSM-0724
S05-R51 -> BSM-0725
S05-R52 -> BSM-0726
S05-R53 -> BSM-0727
S05-R54 -> BSM-0728
S05-R55 -> BSM-0729
S05-R56 -> BSM-0730
S05-R57 -> BSM-0731
S05-R58 -> BSM-0732
S05-R59 -> BSM-0733
S05-R60 -> BSM-0734
S05-R61 -> BSM-0735
S05-R62 -> BSM-0736
S05-R63 -> BSM-0737
S05-R64 -> BSM-0738
S05-R65 -> BSM-0739
S05-R66 -> BSM-0740
S05-R67 -> BSM-0741
S05-R68 -> BSM-0742
S05-R69 -> BSM-0743
S05-R70 -> BSM-0744
S05-R71 -> BSM-0745
S05-R72 -> BSM-0746
S05-R73 -> BSM-0747
S05-R74 -> BSM-0748
S05-R75 -> BSM-0749
S06-R01 -> BSM-0750
S06-R02 -> BSM-0751
S06-R03 -> BSM-0752
S06-R04 -> BSM-0753
S06-R05 -> BSM-0754
S06-R06 -> BSM-0755
S06-R07 -> BSM-0756
S06-R08 -> BSM-0757
S06-R09 -> BSM-0758
S06-R10 -> BSM-0759
S06-R11 -> BSM-0760
S06-R12 -> BSM-0761
S06-R13 -> BSM-0762
S06-R14 -> BSM-0763
S06-R15 -> BSM-0764
S06-R16 -> BSM-0765
S06-R17 -> BSM-0766
S06-R18 -> BSM-0767
S06-R19 -> BSM-0768
S06-R20 -> BSM-0769
S06-R21 -> BSM-0770
S06-R22 -> BSM-0771
S06-R23 -> BSM-0772
S06-R24 -> BSM-0773
S06-R25 -> BSM-0774
S06-R26 -> BSM-0775
S06-R27 -> BSM-0776
S06-R28 -> BSM-0777
S06-R29 -> BSM-0778
S06-R30 -> BSM-0779
S06-R31 -> BSM-0780
S06-R32 -> BSM-0781
S06-R33 -> BSM-0782
S06-R34 -> BSM-0783
S06-R35 -> BSM-0784
S06-R36 -> BSM-0785
S06-R37 -> BSM-0786
S06-R38 -> BSM-0787
S06-R39 -> BSM-0788
S06-R40 -> BSM-0789
S06-R41 -> BSM-0790
S06-R42 -> BSM-0791
S06-R43 -> BSM-0792
S06-R44 -> BSM-0793
S07-R01 -> PGM-0198
S07-R02 -> PGM-0199
S07-R03 -> PGM-0200
S07-R04 -> PGM-0201
S07-R05 -> PGM-0202
S07-R06 -> PGM-0203
S07-R07 -> PGM-0204
S07-R08 -> PGM-0205
S08-R01 -> PGM-0206
S08-R02 -> PGM-0207
S08-R03 -> PGM-0208
S08-R04 -> PGM-0209
S08-R05 -> PGM-0210
S08-R06 -> PGM-0211
S08-R07 -> PGM-0212
S09-R01 -> PGM-0213
S09-R02 -> PGM-0214
S09-R03 -> PGM-0215
S09-R04 -> PGM-0216
S09-R05 -> PGM-0217
S09-R06 -> PGM-0218
S09-R07 -> PGM-0219
S09-R08 -> PGM-0220
S09-R09 -> PGM-0221
S09-R10 -> PGM-0222
S09-R11 -> PGM-0223
S09-R12 -> PGM-0224
S09-R13 -> PGM-0225
S09-R14 -> PGM-0226
S09-R15 -> PGM-0227
S09-R16 -> PGM-0228
S09-R17 -> PGM-0229
S09-R18 -> PGM-0230
S09-R19 -> PGM-0231
S09-R20 -> PGM-0232
S09-R21 -> PGM-0233
S09-R22 -> PGM-0234
S09-R23 -> PGM-0235
S09-R24 -> PGM-0236
S09-R25 -> PGM-0237
S09-R26 -> PGM-0238
S09-R27 -> PGM-0239
S09-R28 -> PGM-0240
S09-R29 -> PGM-0241
S09-R30 -> PGM-0242
S09-R31 -> PGM-0243
S09-R32 -> PGM-0244
S09-R33 -> PGM-0245
S09-R34 -> PGM-0246
S09-R35 -> PGM-0247
S09-R36 -> PGM-0248
S09-R37 -> PGM-0249
S09-R38 -> PGM-0250
S09-R39 -> PGM-0251
S09-R40 -> PGM-0252
S09-R41 -> PGM-0253
S09-R42 -> PGM-0254
S09-R43 -> PGM-0255
S09-R44 -> PGM-0256
S09-R45 -> PGM-0257
S10-R01 -> BEQ-0170
S10-R02 -> BEQ-0171
S10-R03 -> BEQ-0172
S10-R04 -> BEQ-0173
```

### 2.6 本轮新增 findings（`REGX-01` ～ `REGX-18`）

| finding_id | type | severity | 所属账本行 | 落点条目 |
|---|---|---|---|---|
| `REGX-01` | GAP | important | BSM-0555 | BSM「门票主路径是客人通过官方 App/小程序或第三方平台自助购买」条 |
| `REGX-02` | GAP | important | BSM-0564 | 建议落点：§15 平台通用能力（现无内部工单章节） |
| `REGX-03` | GAP | important | BSM-0565 | 建议落点：§15 平台通用能力（现无内部工单章节） |
| `REGX-04` | GAP | important | BSM-0647 | BSM「核销、退款与有效期规则·下单与支付时间」条 |
| `REGX-05` | GAP | important | BSM-0649 | BSM「退款授权与留痕」条 |
| `REGX-06` | GAP | important | BSM-0656 | BSM「积分与成长值获取」条 |
| `REGX-07` | GAP | important | BSM-0700 | BSM「桌台/账单状态与经营规则·下单与支付时间」条 |
| `REGX-08` | GAP | important | BSM-0704 | BSM「销售提成」条 |
| `REGX-09` | GAP | important | BSM-0708 | BSM「销售提成」条 |
| `REGX-10` | GAP | important | BSM-0734 | BSM「每个项目分别建立“内部结算价管理”页面和独立价格目录」条 |
| `REGX-11` | GAP | advisory | BSM-0741 | BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条 |
| `REGX-12` | GAP | advisory | BSM-0742 | BSM「影响结算的规则由各自责任主体在系统中配置、审批和版本化」条 |
| `REGX-13` | CONFLICT | important | BSM-0761 | BSM「收入归属：哪类收入归哪个业态主体·服务点单」条 |
| `REGX-14` | CONFLICT | important | BSM-0762 | BSM「收入归属：哪类收入归哪个业态主体·服务点单」条 |
| `REGX-15` | GAP | important | PGM-0199 | PGM「页面上每个名称、编号、金额、状态、人员」条 |
| `REGX-16` | GAP | advisory | PGM-0205 | PGM「治理顺序」条 |
| `REGX-17` | GAP | important | PGM-0219 | PGM「物理项目与运营公司：多对多协作，不是固定隶属层级·城市项目公司」条 |
| `REGX-18` | GAP | advisory | PGM-0232 | PGM「审批链不由发起人选择档位」条 |

其中 `REGX-01`～`REGX-12` 为各片草稿已起草、经裁决⑦⑧全数采纳者；
`REGX-13`／`REGX-14` 为裁决⑤新增（服务点单收款主体与管理费归属的涉钱口径差异，type=CONFLICT）；
`REGX-15` 为 shard-07 草稿（与 `R10-U-03` 同因）；`REGX-16` 为裁决③新增行的承接缺口；
`REGX-17`／`REGX-18` 为裁决⑥新增（主体级排除清单缺失、审批链跨单据总则缺）。

另按裁决④对既有 `R10-U-03` 追注一处实检结论（**改列不删行**），findings 表因此为
「新增 18 行 ＋ 就地加注 1 处」。

### 2.7 承接矩阵的列填法（本轮口径，供审查核对）

- `prd_path` 只放**单一主路径**（沿用矩阵现状：既有 926 行中多路径写法为 0）；草稿里「路径＋章节」的
  混写串按 `docs/….md` 切分，单归口时把章节文字放 `prd_anchor_or_line`，多归口时**主路径进 `prd_path`、
  原串整体留在 `prd_anchor_or_line`，一字不丢**；无归口的按 `PGM-0196` 先例写「无归口 PRD」。
- `conflict_summary` 只在 `coverage_status != FULL` 时填，内容为该行的承接实检证据；
  `recommended_change` 填对应 finding 的处置建议。FULL 行两列留空（沿用矩阵现状）。
- `phase` 本轮统一填 `regext-expansion`。**这是一处实现决策**：矩阵既有取值里既有审计阶段名
  （`03-governance-and-common` 等 4 种），也有轮次名（`r8-rebuild`，72 行）。306 行横跨票务、酒吧、
  会员、财务、楼宇等多域，塞进任一审计阶段名都会污染该列语义，故按 `r8-rebuild` 先例用轮次名。
  该列无任何工具读取（全库只有 `tools/verify-entry-anchors.mjs` 读这份矩阵，且不读 `phase`）。
- 裁决②：`S03-R05`（→ 见映射表）承接判 FULL 不变，`decision_id` 填 `DEC-R6-26`、
  `conflict_summary` 留一句解释「都进统一工单」与商户服务工单分表裁决之间的张力。

### 2.8 非规则处置表

`docs/prd-audit/rounds/regext/non-rule-dispositions.csv`，94 行（`entry_id` / `entry_name` / `理由`），
UTF-8 BOM ＋ CRLF ＋ 全列加引号，与同目录两份台账形态一致。
94（非规则）＋ 132（规则条目）＝ 226，与 §1.4 差集守恒，**226 条全处置可查**。

### 2.9 收工断言

```
三份 HTML sha256（本轮零改动，与 §1.1 基线逐位一致）
10758753fea57bb1cda2d041960d218acd07e8855829d307514597ab1921aa4c  docs/business-enum-quick-reference-v0.1.html
0a66315af52a98a256181e226e1b42eb9f715896b0c4354da2dec8755d224906  docs/business-system-map-for-operations-v0.1.html
5e5892c43d4ee48b40cf06d92a68ab396ecd9bfb555ac4984f054c285476db69  docs/platform-governance-map-for-operations-v0.1.html
```

| 文件 | 旧 BOM/CRLF/LF/裸CR | 新 BOM/CRLF/LF/裸CR | 行增量 | 原前缀逐字节未变 |
|---|---|---|---|---|
| `authority-rule-ledger.csv` | True / 918 / 918 / 0 | True / 1224 / 1224 / 0 | +306 | 是（纯追加） |
| `html-prd-coverage.csv` | True / 927 / 927 / 0 | True / 1233 / 1233 / 0 | +306 | 是（纯追加） |
| `rounds/r8/findings.csv` | True / 187 / 187 / 0 | True / 205 / 205 / 0 | +18 | 否——唯一一处就地改动即 `R10-U-03` 的 `disposition`，`git diff` 实测 1 删 1 增、其余全为追加 |
| `business-decision-ledger.md` | False / 1565 / 1641 / 0 | False / 1573 / 1649 / 0 | +8 | 是（纯追加 `DEC-R8-68`；该文件本就 CRLF/LF 混排，追加段随文件尾部形态用 CRLF，裸 CR 仍为 0） |

三表恒等式：账本新增 306 ＝ 复验存活 305 ＋ 裁决③补登 1；承接矩阵增量 306 ＝ 账本增量 306；
findings 增量 18 ＝ REGX 条数 18，另有 1 处既有行 disposition 加注（任务书预期为「＋2 处加注」，
**实际编排裁决里点名要求改 disposition 的只有 `R10-U-03` 一处**，裁决②的 `DEC-R6-26` 留痕落在
承接矩阵行而非 findings 行，故此处如实记为 1 处，见「与批准计划的偏差」）。

`git status --short`（无范围外改动）：

```
 M docs/prd-audit/authority-rule-ledger.csv
 M docs/prd-audit/business-decision-ledger.md
 M docs/prd-audit/html-prd-coverage.csv
 M docs/prd-audit/rounds/r8/findings.csv
?? docs/prd-audit/rounds/regext/
```

### 2.10 全量门禁

```
$ npm run verify:predeploy ; echo EXIT=$?
... 15 道子门禁全绿（含 verify:entry-anchors）
EXIT=0
```

首次跑 `verify:predeploy` 因本 worktree 缺 `node_modules` 报 `ERR_MODULE_NOT_FOUND: yaml`，
按本仓 worktree 惯例软链 `~/complex-management/node_modules` 后重跑，`EXIT=0`。
`node_modules` 已被 `.gitignore`，`git status` 不受影响。

### 2.11 本节实际执行的命令

```
python3 ~/regext_load.py          # 归一化 10 片草稿 ＋ 门禁⑤同源逐行复验（不中 0 行）
python3 ~/recheck_missing.py      # 裁决⑨ MISSING/升格断言放宽复验（无翻案）
python3 ~/regext_build.py         # 编号、findings 组装、承接列填法 → ~/regext_plan.json
python3 ~/regext_apply.py         # 步骤0 加注 ＋ 31 批串行落盘，每批真跑门禁
python3 ~/regext_nonrule.py       # 94 行非规则处置表
python3 ~/regext_dec.py           # 决策台账追加 DEC-R8-68
node tools/verify-entry-anchors.mjs   # 追加台账后复跑，EXIT=0
npm run verify:predeploy              # EXIT=0
```

**本轮未执行** `git add` / `git commit` / `git push` / PR。

---

## 三、独立技术审查返工批（2026-08-06）

> 触发：扩登记轮独立技术审查 `TECHNICAL_VERDICT: NEEDS_FIX`，5 项。本节只记返工批实做。
> **本批同样未改三份最终解释权 HTML、未改 `entry-registry.csv`、未改任何 PRD、未 commit。**

返工前基线（返工批开工实测，与本轮落盘收官一致）：

```
$ node tools/verify-entry-anchors.mjs ; echo EXIT=$?
[entry-anchors] 注册表 1210 行；账本 1223 行；承接矩阵 1232 行（同名 1223、账本中无此 rule_id 9）；
被检 Markdown 341 份；条目名引用 990 处（注册表原名 885 处，正文唯一定位 105 处）。
[entry-anchors] PASS
EXIT=0
```

### 3.1 FIX-REGX-01（Blocking）：`BSM-E0508`「人工补录、主管复核」改判为规则

**改前**：判 non-rule，理由「短标签列表项、六要素零命中」，在 `non-rule-dispositions.csv` 第 46 行。

**改判依据（两条，缺一不可）**：

1. **同构条目处置自相矛盾**：本条落在 BSM「必须关注的动作」清单内，同一清单相邻两行
   「核销成功、失败、拒绝、撤销」与「退款、免单、账单调整」在账本里分别是 `BSM-0342`、`BSM-0344`（均为 R8 前既有行），
   本条夹在两者之间却判非规则，同一清单同一句式两种处置。
2. **判据④命中**：正文含「**主管复核**」，即权限/审批/留痕要素，非纯标签。

**改后**：账本新增 1 行（列填法与同清单兄弟行 `BSM-0342`／`BSM-0344` 一致，`approval` 与 `evidence_required` 落判据④）

```
BSM-0794 | 人工补录、主管复核 | L1443 | source_text=「人工补录、主管复核。」
  domain=evidence  subject=主管  object=补录|复核
  approval=人工补录、主管复核。   evidence_required=人工补录、主管复核。   其余列 NOT_STATED
```

**承接检索（实检，非推断）**：`docs/evidence-risk-control-product-prd-v0.1.md`

- §7.2 人工补录：补录规则六条（补录时限可配＋超时不得补录／须附原始记录凭证并经审核／
  一律进主管复核队列、复核通过才生效／涉金额·归属·提成·责任的补录由被影响方之外的主管复核／并列展示并标明「人工补录」）；
- §11 第 1 条 主管复核强制项六项，明列「人工补录」。

两个要素（人工补录、主管复核）均有逐句承接 ⇒ 承接矩阵判 **FULL**，**不加 `REGX-19`**（无缺口可登）。

`non-rule-dispositions.csv` 同批移除该行。

### 3.2 FIX-REGX-03（Important）：`BSM-E0664`「客人 App/小程序」改判为规则

**改前**：判 non-rule，理由「入口一览表清单行、属功能清单／UI 措辞」。

**改判依据**：入口一览表共 7 行同构，其余 6 行（`BSM-0458` 商户 App ～ `BSM-0463` 移动管理端）**全部已在账本**，只余本行未登。

**为什么选「补登第 7 行」而不是「改写 6 个兄弟行的处置」**：6 个兄弟行是 R8 前既有行、已过多轮核验；
补登 1 行即可让 7 行整表统一进入门禁检查⑤的逐字保护面，改动面 1 行 vs 6 行，且不动任何已核验行。

**改后**：账本新增 1 行（列填法对齐兄弟行；本条正文只界定该入口可做的事，故落 `permission` 列）

```
BSM-0795 | 客人 App/小程序 | L1741
  source_text=「客人 App/小程序 游客、会员、潜在客户 官方购票、预定/订台、查订单、查权益、看积分摘要、出示验票二维码；礼物兑换。」
  domain=governance  subject=客人  object=门票|订单|权益|积分|二维码  permission=同 source_text  其余列 NOT_STATED
```

**承接检索（实检）**：`docs/customer-app-miniapp-product-prd-v0.1.md` §3 主要功能逐项对上——
官方购票／预定·订台（含定金）／查我的订单、我的票／查我的权益与使用记录／会员积分摘要／展示验票二维码／礼物目录与自助兑换。判 **FULL**。

`non-rule-dispositions.csv` 同批移除该行。**两条移除后：94 → 92 行。**

### 3.3 FIX-REGX-02（Blocking）：`REGX-13`／`REGX-14` 改判为 HTML 内部口径冲突

**审查指出的病根**：原判「PRD 未对齐 HTML、PRD 同步排下轮」把一个 **HTML 自身的内部冲突**误记成 PRD 的单向欠账。

**实检到的冲突双方（同在 BSM 一份 HTML 内，逐字）**：

| 方向 | 逐字原文 | 出处 |
|---|---|---|
| 收款主体＝酒吧商户 | 服务点单点舞等专项服务的收款主体为该服务发生地所属的酒吧商户：服务费全额进该酒吧商户账户， | 收入归属章（账本 `BSM-0761`） |
| 管理费归酒吧商户 | 管理费部分归该酒吧商户收入并计入其净利润基数 | 收入归属章（账本 `BSM-0762`） |
| 收款主体＝公司 | 客人扫公司统一收款码付款，钱直接进公司账户，财务可对账。 | 服务点单章（账本 `BSM-0193`） |
| 全额进公司账 | 所有服务费由公司统一收款、全额进公司账——不存在钱不进账的情况；分给服务人员的部分，是入账之后按薪资/提成/成本名录列支的支出。 | 服务点单章导语 |

`docs/service-order-product-prd-v0.1.md` §4.1／§8.1 与**后一口径**逐句一致 ⇒ **PRD 并非「未对齐」，而是对齐了冲突的另一侧**；
若照原判把 PRD 单向改成酒吧商户口径，反而会把 HTML 自身的矛盾固化进 PRD。

**改后（两行同法）**：`type` → `HTML-INTERNAL-CONFLICT`；`html_evidence` 追加上表后两条逐字原文；
`disposition` 首句改为「HTML 内部口径冲突，须由用户裁决收款主体与管理费归属，裁决前 PRD 不得单向对齐」，
保留原有的放宽口径复验事实（那部分是实检结果、仍成立），删去已被推翻的「PRD 须向 HTML 对齐、同步排下轮」结论；`status` → `pending-user`。

**裁决未到，本批不改 HTML、不改 PRD。**

### 3.4 FIX-REGX-04（Important）：新增 306 行的 `value_flow` 涉钱回填

判定式（审查给定）扫本轮新增 306 行的 `source_text`：

```
归(该|本|其|娱乐|游乐|传媒|中央|城市|平台|商户|酒吧)|全额进|计入.*基数|由.*承担|不得计入自身收入|应付给|收入归
```

**实扫结果：`value_flow==NOT_STATED` 且命中判定式的共 20 行**（审查估「约 20 行」，实测吻合）。
其中 **19 行判为漏填、已回填**；**1 行判为判定式假阳性、如实保留 `NOT_STATED`**：

- `BSM-0713`「取酒与人工例外」：命中片段是「找不到可承担确认责任的营销主管时流程不得通过」，
  说的是**取酒身份确认的责任归属**，整条无任何金额、收入、成本或分账流转 ⇒ 回填 `value_flow` 会写入假事实，故不填，此处留痕。

回填口径：**只动 `value_flow` 一列**；填入内容一律是该行 `source_text` 的**逐字连续子串**（脚本内 `assert FILL[rid] in fields[4]` 硬校验），
不改写、不概括、不跨行拼接；`source_text` 与其余 16 列一字未碰（脚本对每一行先做 CSV 往返自等校验 `emit(fields)==原行`，不等即中止）。

逐行清单（19 行）：

| rule_id | 条目 | 回填进 `value_flow` 的逐字摘句 |
|---|---|---|
| `BSM-0610` | 域内通票不按经营域比例拆分 | 两者一律在拆分给各域之前从整单扣除且只扣一次，由各域按分配份额自然共担、不由该笔业务的收款主体单独承担，域内通票同样适用；官方渠道售出的票不扣销售渠道费，但仍扣通路费。 |
| `BSM-0620` | 收费权益、豪华套票内小食或特殊权益 | 哪个域主管公司将权益对客价格或域内分配价格定得低于内部结算价，哪个主体承担差额；剩余也归该责任主体。 |
| `BSM-0621` | 门票综合服务费只适用于酒吧相关票务并归娱乐平台公司 | 门票综合服务费只适用于酒吧相关票务并归娱乐平台公司。 |
| `BSM-0622` | 门票综合服务费只适用于酒吧相关票务并归娱乐平台公司 | 单一酒吧门票由酒吧商户定价、销售并确认已核销部分的门票收入，票款由娱乐平台公司统一收款后结付，到期未核销部分不归酒吧商户、按未核销票款规则归娱乐平台公司； |
| `BSM-0637` | 定价、售票与收款主体 | 收款主体不等于收入归属主体：娱乐平台公司只把拆分后归属酒吧域的份额确认为自身收入，其余各域份额自收款之日起即为应付给对应域主管公司的款项，不得计入自身收入、不得挪用。 |
| `BSM-0658` | 积分与成长值获取 | 基础积分和等级加成积分成本均由消费商户承担。 |
| `BSM-0696` | 通票中的酒水、小食和其他权益必须分别绑定实际提供方 | 娱乐平台公司决定或出资的酒吧域权益由娱乐平台公司承担核销成本，其他域决定或出资的权益由对应域主管公司承担。 |
| `BSM-0698` | 酒吧域内采用内部结算价还是固定金额、固定单价 | 娱乐平台公司把对客权益价或域内分配价定得低于该权益锁定的内部结算价时，由娱乐平台公司按内部结算价向实际提供方足额结算并自行承担差额；定得高于内部结算价时，高出内部结算价的部分也归娱乐平台公司。 |
| `BSM-0702` | 销售提成 | 按员工岗位判定费率：设有「销售岗」标识的员工按销售人员费率计提，其余按非销售人员费率计提 |
| `BSM-0708` | 销售提成 | 凡属于付给酒吧的收入部分，酒吧按实退金额全额退回，对应的销售提成按订单最终保留收入重新计算并冲回差额；凡属于服务费、小费与表演报酬的部分，因退款均发生在发放之前，一律按订单最终保留收入重新计算并冲回差额，不由酒吧承担。 |
| `BSM-0715` | 服务费 | 服务费由商户代收，按后台选定的分配方式发放给当班服务提供人；是否扣留一部分不予发放、扣留多少由该商户决定 |
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-61，本文不再重复记载。
| `BSM-0718` | 服务费 | 服务费不计入最低消费，也不计入销售提成基数。 |
| `BSM-0720` | 每个商户必须归入一个业态 | 业态用于确定经营域、主管公司和域内结算规则 |
| `BSM-0760` | 单一酒吧门票 | 单一酒吧门票已核销部分对应酒吧商户，到期未核销部分按票种所属经营域归娱乐平台公司；票款由娱乐平台公司统一收款后结付。娱乐平台公司不取得已核销门票的收入，只按生效规则另收可封顶的门票综合服务费。 |
| `BSM-0761` | 收入归属：哪类收入归哪个业态主体·服务点单 | 服务点单点舞等专项服务的收款主体为该服务发生地所属的酒吧商户：服务费全额进该酒吧商户账户， |
| `BSM-0762` | 收入归属：哪类收入归哪个业态主体·服务点单 | 管理费部分归该酒吧商户收入并计入其净利润基数，其余份额记到服务提供人名下、月结进其薪资提成并按劳务报酬代扣个税。 |
| `BSM-0764` | 酒吧通票 / 跨域酒吧份额 | 酒吧通票/跨域酒吧份额由娱乐平台公司按酒吧域规则负责；相应门票综合服务费归娱乐平台公司且不得封顶。 |
| `BSM-0776` | 金额尾差 | 多方分配产生的尾差单独留痕，并由该笔业务实际承担结算责任的主体承担，不得默认归城市项目公司或平台。 |

与备份逐字节比对：`diff` 仅 19 删 19 增，无其他行变动。

### 3.5 FIX-REGX-05（Important）：`PGM-0196` 由 MISSING 收敛为 PARTIAL

本轮 `BSM-0553` 行（BSM 侧同一条「数据可溯源、全链可点击」全局原则）已实检到承接并判 PARTIAL，
但当时在 `conflict_summary` 里写明「是否回改 R10-U-03 由合并代理并案裁定，本行不擅自改动 R10 结论」——该并案本轮未做，即审查指出的口径分裂。

**改后**（三处，同源同判）：

| 位置 | 改前 | 改后 |
|---|---|---|
| 承接矩阵 `PGM-0196` | `MISSING` ／ `无归口 PRD` ／ severity `important` | `PARTIAL` ／ `docs/product-requirements-v0.1.md` §14 已确认业务规则·第 40 条 ／ severity `advisory` |
| findings `R10-U-03` | severity `important`，文字称「无归口・零承接缺口」 | severity `advisory`，文字改为「原则本体已承接，残余缺口＝效力范围声明未承接 ＋ PGM 侧归口未指认」 |
| 承接矩阵 `BSM-0553` | `conflict_summary` 末尾「⚠ 与 R10 待并案……本行不擅自改动 R10 结论」 | 同句改为「✅ 已并案（本返工批）……两行残余缺口一致」（**仅替换该末句，前段实检证据一字未动**） |

`BSM-0553` 那一处属本项收敛的必要一致性收尾（不改它会留下一句与新状态矛盾的「待并案」），在此显式披露。

### 3.6 返工批逐步门禁与收官验证

每步落盘后真跑条目锚点门禁：

| 步骤 | 动作 | 账本 | 承接矩阵 | 门禁 EXIT |
|---|---|---|---|---|
| 返工-1 | FIX-REGX-04 `value_flow` 回填 19 行 | 1223 | 1232 | 0 |
| 返工-2 | FIX-REGX-01＋03 账本 ＋2、矩阵 ＋2、non-rule −2 | 1225 | 1234 | 0 |
| 返工-3 | FIX-REGX-02＋05 findings 3 行、矩阵 2 行就地改写 | 1225 | 1234 | 0 |
| 返工-4 | 决策台账追加 DEC-R8-69 | 1225 | 1234 | 0 |

### 3.7 返工批的字节级形态守恒

| 文件 | BOM | 行尾 | 收官实测 |
|---|---|---|---|
| `authority-rule-ledger.csv` | 有 | 全 CRLF | 1226 行（表头 1 ＋ 数据 1225） |
| `html-prd-coverage.csv` | 有 | 全 CRLF | 1235 行（表头 1 ＋ 数据 1234） |
| `rounds/r8/findings.csv` | 有 | 全 CRLF | 205 行（表头 1 ＋ 数据 204，**行数未变**，仅 3 行就地改写） |
| `rounds/regext/non-rule-dispositions.csv` | 有 | 全 CRLF | 93 行（表头 1 ＋ 数据 92） |
| `business-decision-ledger.md` | 无 | 混排，**LF-only 恒为 76** | 纯追加，CRLF 1573 → 1581、LF-only 76 → 76 |
| `rounds/regext/worklog.md` | 无 | 全 LF | 纯追加 |

三份 HTML 与条目注册表 sha256 返工前后逐位不变（`10758753…` ／ `0a66315a…` ／ `5e5892c4…` ／ 注册表 `d81b1f8b…`）。

### 3.8 用户裁决到达后的收尾（2026-08-06，接 §3.3）

§3.3 落盘时裁决未到、两行挂 `pending-user`。裁决当日到达，**结论为甲：两处是同一主体、不构成矛盾**——
服务点单章「公司统一收款、全额进公司账」的「公司」**即该服务发生地酒吧的经营主体**，
该句的对立面是**服务人员个人私收**（防钱不进账），收入归属表只是把同一个「公司」点名成酒吧商户；
**管理费部分归该酒吧商户收入并计入其净利润基数**。

即 §3.3 实检到的「两侧文字不一样」属实，但**推论「HTML 自相矛盾」被裁决推翻**：两侧指同一主体，差的只是点名精度。

| 位置 | 改前（§3.3 落盘态） | 改后（本节） |
|---|---|---|
| findings `REGX-13`／`REGX-14` `status` | `pending-user` | `resolved` |
| 两行 `disposition` | 「HTML 内部口径冲突，须由用户裁决……裁决前 PRD 不得单向对齐」 | 裁决原意（同一主体、公司＝该酒吧经营主体、管理费计入净利润基数）＋ 两项遗留 |
| `type` | `HTML-INTERNAL-CONFLICT` | **未动**（裁决只裁结论，两行的发现历史保留原样，避免抹掉「曾疑为内部冲突」这一事实） |

**遗留两项排下轮，本批一律不做**：㈠ 三份 HTML 加互认括注（C 类草案逐句批）；
㈡ PRD 把「公司」点名到酒吧商户并补「计入净利润基数」句。裁决全文见决策台账 `DEC-R8-70`。

收尾验证（真跑，见本轮报告）：条目锚点门禁 `EXIT=0`（账本 1225 / 承接矩阵 1234）、`npm run verify:predeploy` `EXIT=0`、
三份 HTML 与条目注册表 sha256 四个逐位不变、决策台账 LF-only 恒 76、`git status` 无 `docs/prd-audit` 之外改动、未 commit。
