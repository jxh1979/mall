# Domain Docs

工程类 skill 探索代码时，应如何消费本项目的领域文档。

## 探索前必读

- **`CONTEXT.md`** —— 仓库根，中文业务术语表。**探索代码前必读**。
- **`docs/adr/`** —— 读与当前改动相关的 ADR。本仓库尚无此目录。

文件不存在时**静默继续**，不要提示缺失，也不要建议提前创建。
`/domain-modeling`（经 `/grill-with-docs` 与 `/improve-codebase-architecture` 触达）会在术语或决策真正被裁定时惰性创建。

## 布局：single-context

```
/
├── CONTEXT.md                 ← 已存在
├── docs/adr/                  ← 尚未创建，按需惰性生成
├── apps/web/
├── backend/
└── tools/
```

判定依据：`package.json` 的 `workspaces` 只有 `apps/web` 一个包，无 `packages/*/src`，无 `CONTEXT-MAP.md` —— 不构成 multi-context。

## 词表之上还有更高权威

`CONTEXT.md` 开头已声明：业务最终解释权属于三份 HTML

1. `docs/business-system-map-for-operations-v0.1.html`
2. `docs/platform-governance-map-for-operations-v0.1.html`
3. `docs/business-enum-quick-reference-v0.1.html`

**这三份 HTML 的改动必须先经用户逐句批准，任何 skill 不得自行编辑。**
PRD、开发设计、接口、数据库、UI、测试和实现均不得反向改写其中定义。

## 用词表里的词

输出里出现领域概念时（issue 标题、重构提案、假设、测试名），一律用 `CONTEXT.md` 里的定义词。
不要漂移到词表明确禁止的别名（例如：「娱乐运营平台」已废止，一律称**娱乐平台公司**；
「酒吧 SPV／酒吧经营主体」一律理解为**各酒吧商户**）。

概念不在词表里，是个信号——要么你在造项目不用的词（重新考虑），要么是真缺口（记下来交给 `/domain-modeling`）。

## ADR 冲突要挑明

输出与现有 ADR 矛盾时，明说，不要静默覆盖：

> _与 ADR-0007（event-sourced orders）矛盾 —— 但值得重开，因为……_
