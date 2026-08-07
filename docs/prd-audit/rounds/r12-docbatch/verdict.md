# R12-docbatch(G-3 专项批)验收结论

- AUDIT_BASE_SHA: e6ab52d9
- AUDITED_TREE_HASH: 0a54f2b88b7f5e969ef4a40d2a3af80aaae28bf7
- 批次范围: `phase=r8-rebuild` 的 59 条 PARTIAL 逐句人工核验
- 前置轮: R11(`docs/prd-audit/rounds/r11/verdict.md`,EVIDENCE_LEVEL=L1_REPO_VERDICT)

## 工作面与结果

59 条原状态清一色 PARTIAL(仅经 R8 机检「14 字滑窗」承接,未逐句核验)。
逐句核验后初判:FULL 37 / PARTIAL 18 / CONFLICT 4。其中:

- 2 条 CONFLICT 的缺口已在本批修复,经独立 Verifier 逐句复核后改判 FULL;
- 1 条(`BEQ-0131`)经复核认定为前序误读,排除 CONFLICT,但因另存三处与计价时点
  无关的承接缺口,降为 PARTIAL 而非 FULL;
- 剩 1 条 CONFLICT(`BEQ-0137`)待用户裁决。

最终四态(全表 1234 条):FULL 1116 / PARTIAL 45 / CONFLICT 1 / STALE 69 / MISSING 3。

**机检的错是双向的**:38 条本该 FULL 被判 PARTIAL,同时漏掉 3 条真 CONFLICT
(初判 4 条,其中 1 条经复核不成立)。

## 实际改动

| 文件 | 改动 |
|---|---|
| `docs/prd-audit/html-prd-coverage.csv` | 59 行判定写回(仅 4 列:`prd_anchor_or_line` / `conflict_summary` / `coverage_status` / `prd_path`);占位串「14 字滑窗」57 处清零 |
| `docs/prd-audit/rounds/r8/findings.csv` | 关闭 6 条过期 finding(R8-F01~F06),新增 R12-U-01/02 |
| `docs/bar-business-operations-product-prd-v0.1.md` | BEQ-0150:删「按人数」活口 2 处、补计算基准与结账通则、头部增 D-20260722-8 收口声明 |
| `docs/amusement-business-operations-product-prd-v0.1.md` | PGM-0193:`:187`「容量记在哪」列改为现行口径,措辞逐字取自 PGM HTML `:478` |
| `docs/prd-audit/rounds/r12-docbatch/` | 核验分片 C1~C7 与本文件 |

## 保护面

三份 HTML 相对基线**零改动**,sha256 与 `e6ab52d9` 逐字节一致:
`edb0b993`(BSM) / `1153b986`(PGM) / `a1b5e023`(ENUM)。
越界扫描零命中(无 `backend/` `apps/` `openapi/` `tools/` `.github/` 改动)。
`authority-rule-ledger.csv` / `entry-registry.csv` / `business-decision-ledger.md` 零改动。

## 门禁

`verify:entry-anchors` / `verify:html-ledger-sync` / `verify:html-param-tier` /
`verify:delivery-gates-docs` 全部 exit 0。

`verify:entry-doc-baseline` exit 1 —— 既存漂移,其输入文件本批一份未碰,
且不在 `verify:predeploy` 链内。记 Advisory,非本批引入。

## 返工

Verifier 判 `TECHNICAL_VERDICT: PASS`,无 Blocking,两条 Important 已全部返工完成:

- **R12-FIX-01**:bar PRD 因本批自身在 `:5-7` 净增 2 行,导致按基线写的锚点当场失效。
  4 条 CSV 行 + 2 份分片共订正 26 个锚点,逐条 `sed` 实读命中。
  另修 2 处本批自身笔误(`:598` 指向空行,真实 `:601`;映射非统一 +2,实为 +1 与 +3 两段)。
- **R12-FIX-02**:`BEQ-0150` / `PGM-0193` 改判 FULL,`conflict_summary` 由现在时改写为
  过去时 + 落点(原文描述的缺口在库中已不存在)。
- **R12-FIX-03**:`BEQ-0131` 计价时点冲突经溯源复核不成立,改判 PARTIAL(非 FULL ——
  三处承接缺口实测为真,判 FULL 会把一道「未获批内部结算价不得开通权益或券」的
  开通闸抹成已承接);同批改写 `BEQ-0143` 中指向该已证伪结论的失效引用。

## FINAL_VERDICT: PASS

## 已排除 CONFLICT(不需裁决)

**BEQ-0131** —— 前序判定误读。ENUM「发券与核销结算」条(`business-enum-quick-reference-v0.1.html:75`)
已明定补偿按发券时锁定的内部结算价计算,「一律」为强制口径词;ENUM「项目内部结算价」条
(`:51`)末句属**封账条**(已结出的账不得追溯改价)而非计价条,其镜像句 BSM
(`business-system-map-for-operations-v0.1.html:1499`)「核销或结算时把当时价格固化到流水,
历史不得手改」宾语写全,立法出处为决策台账 `DEC-ORG-2026-07-28-12`「改价走草案→他人复核
转正→对后续生效;核销/结算固化当时价且历史只读」。`git log -S` 溯源确认:封账句由
`f4f323bb`(2026-08-01)写入,计价句由 `6955dfa5`(2026-08-03,用户逐条批准)后补,
系为消除歧义专门补入。**PRD 承接正确,HTML 不改。**
降为 PARTIAL 的原因是本条另有三处与计价时点无关的承接缺口(见结转节),不是冲突。

## 待用户裁决(本批未解决,不构成合并阻塞)

剩余 1 条 CONFLICT 为「PRD 正确、待改的是 HTML」,本轮按范围排除(不改 HTML):

- **BEQ-0137** —— ENUM `L66`「发行成本由售票收款主体承担」与同文件另四处「各域按份额共担」
  相反。`DEC-R7-17`(2026-08-05)已裁定共担并否决收款主体承担,但该裁决记录称
  「不写入 HTML / HTML 未改」—— 实测 `git log -S` 显示那句 2026-08-03 已写入,
  裁决基于错误的事实前提,只更正了 PRD。**用户 2026-08-07 已重申:票卖完后预扣一笔积分成本,
  与通路费/渠道费同机制。** 待出逐句草案改 HTML `L66` 并订正台账失实记录。

## 结转(不得当作已解决)

- **BEQ-0131 的三处承接缺口**(本条降为 PARTIAL 的唯一原因,实测
  `docs/merchant-points-settlement-rules-v1.0.md` 全文零命中):
  ①PRD 漏「零售商户」整类与「联营、租赁」两模式(`:172` 只写到独立餐饮商户);
  ②漏「业态为『其他』的商户,由其商户档案上指定的主管公司审批」整句(「商户档案」全文零命中);
  ③商户级开通闸「任何商户在没有获批内部结算价之前,不得为其开通需要该价格结算的权益或券」
  缺失,`:169` 只承接到商品规格一级。三句在 ENUM `:51` 与 BSM `:1499` 均有原文。
- **`BSM L818` 未镜像 2026-08-03 补入的计价句** —— 仍是压缩版「锁定项目和全部价格及规则版本」,
  与 ENUM `:75` 的「补偿金额一律按发券时锁定的该项目该商品内部结算价计算」同向不冲突,
  属未同步镜像点。
- **2026-08-03 那批 HTML 修订未在两份决策台账立 DEC 编号** —— 唯一裁决证据是 commit
  `6955dfa5` 的 message 与 HTML 正文。与 `DEC-R7-17`「台账称 HTML 未改、实测已改」同类病,
  该病已两次致误判。
- `html-prd-coverage.csv` 中约 25 行 R11 期遗留的陈旧 bar 行号锚点
  (`BSM-0561/0579/0580/0686…0719` 等,均不在本批 59 行内)。抽查基线:
  `L596`/`L758`/`L159` 指向空行,`L624` 自称「低消规则」实为「吧台买酒优先线上支付」,
  `L444` 自称「平板点酒」实为存酒隐私同意。
- `verify-entry-anchors.mjs` **只校验 Markdown 正文的行号锚点,不校验 CSV 内行号** ——
  上述漂移无任何门禁可拦。R9 已废除正文行号锚点制度,`html-prd-coverage.csv` 是制度外残留面。
- `recommended_change` 列 22 条 PARTIAL/CONFLICT 仍为模板句,可执行改法都塞在 `conflict_summary`。
