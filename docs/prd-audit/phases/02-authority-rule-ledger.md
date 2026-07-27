# 阶段 2：三个 HTML 原子规则账本

状态：完成
日期：2026-07-27

## 输入基线

- 起始提交：`4236de6fbf0b45235606301fd22e6b3f2f0ae108`
- 分支：`audit/prd-phase2-rule-ledger`
- 起始工作区：干净
- 三个 HTML 哈希与阶段 1 一致。
- worktree 已执行独立 `npm ci`，没有链接主工作区依赖。

## 提取方法

从当前三个 HTML 重新解析，不直接复制 2026-07-23 旧账本：

- 捕获列表项、数据表数据行、明确的业务说明块和问题块。
- 跳过纯表头和明显只用于引导阅读的句子。
- 每条规则保留当前文件、源代码行号和原文。
- 规则 ID 按当前来源顺序稳定编号：`BSM-*`、`PGM-*`、`BEQ-*`。
- 主体、对象、触发条件、前置条件、状态变化、价值流、权限、审批、职责分离、失败恢复和证据要求只从原文明确内容中提取。
- 原文没有明确某个第一性原理维度时写 `NOT_STATED`，不推测或补写业务规则。
- `html-prd-coverage.csv` 在本阶段只建立固定表头；后续读完对应 PRD 后才写真实覆盖行。

## 规则统计

| 来源 | 规则数 |
|---|---:|
| `business-system-map-for-operations-v0.1.html` | 530 |
| `platform-governance-map-for-operations-v0.1.html` | 192 |
| `business-enum-quick-reference-v0.1.html` | 122 |
| 合计 | 844 |

按主要领域分类：

| 领域 | 规则数 |
|---|---:|
| governance | 114 |
| bar | 83 |
| cross-domain | 78 |
| ticketing | 74 |
| evidence | 56 |
| finance | 54 |
| terminal | 40 |
| building | 40 |
| supply | 36 |
| thirdparty | 35 |
| amusement | 32 |
| workforce | 32 |
| realestate | 30 |
| leasing | 27 |
| dividend | 26 |
| data | 26 |
| customer | 22 |
| merchant | 16 |
| service | 12 |
| member | 11 |

领域是审计路由标签，不改变规则原文，也不限制一条规则在多个 PRD 中建立覆盖关系。

## 第一性原理字段完整性

所有 `844` 行均包含全部固定列。`NOT_STATED` 表示当前 HTML 未在该条规则中明确对应维度，不表示该维度不重要：

| 字段 | NOT_STATED 行数 |
|---|---:|
| subject | 488 |
| object | 278 |
| trigger | 536 |
| preconditions | 640 |
| state_transition | 583 |
| value_flow | 632 |
| permission | 735 |
| approval | 705 |
| separation_of_duties | 843 |
| failure_recovery | 718 |
| evidence_required | 699 |

这些缺省值将在后续顺从性与对抗性审计时用于检查 PRD 是否补充了 HTML 未规定的细节，以及这些补充是否需要用户裁决。

## 决策状态

- 当前 HTML 显式未确认问题块：0。
- `AUTHORITATIVE`：844。
- 历史续接待裁决：2，已写入 `business-decision-ledger.md`，不伪装成 HTML 规则。

## 结构验证

- 账本行数：844。
- 规则 ID 重复：0。
- 文件、行号和原文为空：0。
- 精确重复的文件＋行号＋原文：0。
- 覆盖矩阵表头与计划一致。
- 当前阶段未写入任何未经 PRD 审计的覆盖结论。
