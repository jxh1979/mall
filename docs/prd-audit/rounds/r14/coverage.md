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
