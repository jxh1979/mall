# 历史备份与当前 PRD 体系差异

日期：2026-07-27
当前基线：`599dc9e8a9eb56f70dde7f7629e1087d04b76a8c`
备份基线：`fb29241de5b4954102e85b281e42ac2dae623f97`

## 结论

`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/` 是历史审计续接包，不是 Git 分支或 worktree。

其 `work/source-hashes.csv` 登记的 38 份快照与当前仓库比较：

| 分类 | 数量 | 使用方式 |
|---|---:|---|
| UNCHANGED_REUSABLE | 37 | 可复用历史分析，但仍须进入本轮原子规则覆盖审计 |
| CHANGED_REVIEWED | 1 | 只复用未被变化推翻的历史裁决 |
| MISSING_CURRENT | 0 | 无 |

唯一变化文件：

- `docs/business-enum-quick-reference-v0.1.html`
- 备份哈希：`d5f6ba58d31141d7c0bc389b254bf751f46776a56381f3d400b5791c9aa355cf`
- 当前哈希：`3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648`
- 当前变化位于备份基线之后；阶段 2 必须根据当前文本重新建立枚举规则，不沿用旧行号或旧摘要。

## 可复用内容

- 2026-07-23 的历史业务裁决原文。
- 三个 HTML 的旧版原子规则提取方法和字段设计。
- 37 份哈希未变快照的历史问题线索。

“可复用”不等于“已验证”或“已覆盖”。所有内容都必须回链到当前 HTML 或当前用户裁决。

## 仅作历史证据

- `phase1-prd-audit-design.md` 和 `phase1-prd-audit-execution-plan.md` 已被当前设计与计划取代。
- `work/authority-rule-ledger.csv` 使用旧版枚举 HTML，不得直接成为当前规则账本。
- `work/findings-working.md` 是历史问题线索，不得直接写入最终报告。
- `REINSTALL-RESUME.md` 的暂停点转入当前业务裁决台账。

## 仍待裁决

历史续接明确留下两个积分清算问题：

1. 每个净积分对应的费用金额或计价方式。
2. 消费者兑换商户虚拟券后，积分是否先冻结，直至实际核销和商户履约才计入商户承接积分。

这两个问题不能在阶段 1 中自行回答，后续按单问题裁决流程处理。

## 备份完整性

- 归档：`complex-management-prd-audit-phase1-reinstall-backup-20260723.tar.gz`
- SHA-256：`abad2f2a55837cf451e5a26b45ef0939b3406a8d7cefd47055aa0ed46503654c`
- `gzip -t`：通过
- 归档成员：51
- 展开目录文件：47
