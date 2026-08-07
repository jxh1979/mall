# R12-docbatch(G-3 专项批)验收结论

- AUDIT_BASE_SHA: e6ab52d9
- AUDITED_TREE_HASH: 7439dac9337bf175e63855c54a2970f3e59f68d9
- 批次范围: `phase=r8-rebuild` 的 59 条 PARTIAL 逐句人工核验
- 前置轮: R11(`docs/prd-audit/rounds/r11/verdict.md`,EVIDENCE_LEVEL=L1_REPO_VERDICT)

## 工作面与结果

59 条原状态清一色 PARTIAL(仅经 R8 机检「14 字滑窗」承接,未逐句核验)。
逐句核验后判定:FULL 37 / PARTIAL 18 / CONFLICT 4。
其中 2 条 CONFLICT 的缺口已在本批修复,经独立 Verifier 逐句复核后改判 FULL。

最终四态(全表 1234 条):FULL 1116 / PARTIAL 44 / CONFLICT 2 / STALE 69 / MISSING 3。

**机检的错是双向的**:38 条本该 FULL 被判 PARTIAL,同时漏掉 4 条 CONFLICT。

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

## FINAL_VERDICT: PASS

## 待用户裁决(本批未解决,不构成合并阻塞)

剩余 2 条 CONFLICT 均为「PRD 正确、待改的是 HTML」,本轮按范围排除(不改 HTML):

- **BEQ-0131** —— ENUM `L51`「核销或结算时锁定当时版本」与 `L75`「补偿金额一律按发券时
  锁定的内部结算价计算」对同一对象给出相反时点。已核实:代码现行行为等于 `L51`,
  且与 `DEC-ORG-2026-07-28-12`(用户 2026-07-28 拍板「核销/结算固化当时价」)同侧;
  `L75` 那句为 2026-08-03 逐条批准时写入,两份台账零对应裁决。真实数据零(0 券定义/0 券/0 核销)。
- **BEQ-0137** —— ENUM `L66`「发行成本由售票收款主体承担」与同文件另四处「各域按份额共担」
  相反。`DEC-R7-17`(2026-08-05)已裁定共担并否决收款主体承担,但该裁决记录称
  「不写入 HTML / HTML 未改」—— 实测 `git log -S` 显示那句 2026-08-03 已写入,
  裁决基于错误的事实前提,只更正了 PRD。**用户 2026-08-07 已重申:票卖完后预扣一笔积分成本,
  与通路费/渠道费同机制。** 待出逐句草案改 HTML `L66` 并订正台账失实记录。

## 结转(不得当作已解决)

- `html-prd-coverage.csv` 中约 25 行 R11 期遗留的陈旧 bar 行号锚点
  (`BSM-0561/0579/0580/0686…0719` 等,均不在本批 59 行内)。抽查基线:
  `L596`/`L758`/`L159` 指向空行,`L624` 自称「低消规则」实为「吧台买酒优先线上支付」,
  `L444` 自称「平板点酒」实为存酒隐私同意。
- `verify-entry-anchors.mjs` **只校验 Markdown 正文的行号锚点,不校验 CSV 内行号** ——
  上述漂移无任何门禁可拦。R9 已废除正文行号锚点制度,`html-prd-coverage.csv` 是制度外残留面。
- `recommended_change` 列 22 条 PARTIAL/CONFLICT 仍为模板句,可执行改法都塞在 `conflict_summary`。
- BEQ-0131 附带三处承接缺口(PRD 漏「零售商户」整类与「联营/租赁」两模式、
  漏「其他业态由商户档案指定主管公司审批」整句、商户级内部结算价开通闸只承接到商品规格一级)。
