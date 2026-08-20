# R14 覆盖表（分批累积）

基线：AUDIT_BASE_SHA = ff2037a1c1e40a97e5979a5517451c7bfe409d16 ｜ 分母 = 27（track=MAIN && document_type=prd 26 + 用户裁决纳入 operation-traceability）

## R14-b01 · 结算钱路（3 文件 / 3 Auditor + 1 Consolidator）

| 文件 | 行数@ff2037a1 | CONFORMANCE | ADVERSARIAL | FIRST_PRINCIPLES | R13 追溯 | 本批 finding |
|---|---|---|---|---|---|---|
| docs/finance-settlement-product-prd-v0.1.md | 1048 | ✅ 8 条 | ✅ 6 条 | ✅ 2 条 | ✅ 7 commit / 29 hunk | 17（+1 Consolidator 补登） |
| docs/settlement-rebuild-prd-v1.0.md | 1163 | ✅ 6 条 | ✅ 5 条 | ✅ 5 条 | ✅ 4 commit / 14 hunk | 16（其中 1 条消解撤销） |
| docs/dividend-engine-prd-v0.1.md | 352 | ✅ 9 条 | ✅ 5 条 | ✅ 3 条 | ✅ 3 commit / 15 hunk | 17 |

**三关全部有 finding，无「本关无发现」情形，故无须援引「无发现 + 依据」条款。**

**Counting contract（承接行，三 shard 同口径）**：计数对象＝`docs/prd-audit/html-prd-coverage.csv` 物理数据行（RFC4180 + utf-8-sig，跳表头，全表 1234 行）；过滤＝`prd_path` 含目标文件名子串；不去重；时点＝`ff2037a1`。
- finance-settlement **122** 行（FULL 111 / STALE 8 / PARTIAL 3；rule_id 全唯一；多落点 2）
- settlement-rebuild **52** 行（FULL 41 / PARTIAL 7 / STALE 4；rule_id 全唯一；多落点 1）
- dividend-engine **20** 行（FULL 19 / STALE 1；rule_id 全唯一；多落点 0）
三数与「BSM 章节条目数」「HTML 分句数」等口径**不互换、不相减**。Consolidator 已用同一脚本独立复算，三数逐位一致。

**本批新增 finding：52 条 open + 1 条 closed**（三 shard 原始 50 − SR-06 撤销 + CON-01/02/03）。
**CODE_FOLLOWUP_DELTA：3 条**（R14-CF-01 ← R14-DE-10；R14-CF-02 ← R14-CON-03；R14-CF-03 ← R14-FS-09 用户裁决恢复待做能力）。
**D 类：0 条**（原 3 条经 Consolidator 代跑全部闭环）。
**分母推进：3 / 27。**

## R14-b02 · 票务权益与外部接入（3 文件 / 3 Auditor + 1 Consolidator）

| 文件 | 行数@ff2037a1 | CONFORMANCE | ADVERSARIAL | FIRST_PRINCIPLES | R13 追溯 | 本批 finding |
|---|---|---|---|---|---|---|
| docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 643 | ✅ 12 条 | ✅ 5 条 | ✅ 5 条 | ✅ 1 commit / 3 hunk | 22 |
| docs/third-party-business-access-product-prd-v0.1.md | 310 | ✅ 9 条 | ✅ 2 条 | ✅ 2 条 | ✅ 2 commit / 8 hunk | 13 |
| docs/service-order-product-prd-v0.1.md | 315 | ✅ 7 条 | ✅ 4 条 | ✅ 2 条 | ✅ 3 commit / 8 hunk | 13 |
| （Consolidator 补登，目标为台账，不计入任一 PRD 的 lens 归属） | — | ✅ 2 条 | — | — | — | 2 |

**三关全部有 finding，无「本关无发现」情形，故无须援引「无发现 + 依据」条款。**

**Counting contract（承接行，三 shard 同口径，Consolidator 用同一脚本独立复算，三数逐位一致）**：计数对象＝`docs/prd-audit/html-prd-coverage.csv` 物理数据行（RFC4180 + utf-8-sig，跳表头，全表 1234 行）；过滤＝`prd_path` 按 `;` 切分后包含目标文件路径的行（含多文件共享行）；不去重；时点＝`ff2037a1`。
- ticketing **102** 行（FULL 85 / STALE 14 / PARTIAL 3；14 行 entry_name=ORPHANED，回 arl 查 decision_status 全为 SUPERSEDED；3 条 PARTIAL＝BEQ-0124/0130/0132）
- third-party **36** 行（FULL 35 / PARTIAL 1；含多路径合并行 1 行 `ticketing;bar;third-party`）
- service-order **22** 行（全 FULL）
三数与「BSM 章节条目数」「HTML 分句数」等口径不互换、不相减。ticketing 的「BSM 票务权益 section 56 行」与「本文件承接面 102 行」是两个口径，任务包所称 56 经复算成立，但不等于本文件的全部承接面。

**本批新增 finding：50 条，全部 status=open**（三 shard 原始 48 ＋ Consolidator 补登 CON-04/CON-05；零撤销、零 closed）。
**audit_lens 分布（从本批 CSV 反算）：CONFORMANCE 30 / ADVERSARIAL 11 / FIRST_PRINCIPLES 9。**
**severity 分布：P0 2 / P1 24 / P2 24。**
**disposition 分布：A 38 / B 8 / C 0 / D 0 / CODE-FOLLOWUP 4**（R14-TK-16 / R14-TK-17 依编排裁定 OB2-1/OB2-2 采 A；R14-TP-11 经 D→B 采信并由裁决 27 结案，仍计 B；详见 rulings-b02.md）。
**CODE_FOLLOWUP_DELTA：4 条**（R14-CF-04 ← R14-SO-13；R14-CF-05 ← R14-TK-21；R14-CF-06 ← R14-TK-20；R14-CF-07 ← R14-TK-22），累计 7 条。
**D 类：0 条**（原 1 条经 Consolidator 代跑闭环）。
**跨批发现 2 条**：R14-SO-02（目标文件属 b05）、R14-SO-03（目标文件属 b01 已审），均不推进分母。
**分母推进：6 / 27。**

**b01 遗留的两处计数订正（本批复算发现，供 R14-final 一并处理）**：① `findings.csv` 中 `R14-DE-10` 的 `audit_lens` 值为 `CODE-FOLLOWUP`，不在三值词表内，须改为其来源关；② b01 coverage.md 记「52 条 open + 1 条 closed」，按 CSV 实际 status 分布为 51 open + 1 closed-pending-fix（R14-SR-05）+ 1 closed（R14-SR-06）。
