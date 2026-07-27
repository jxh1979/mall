# 阶段 1：基线、范围和历史续接

状态：完成
日期：2026-07-27

## 远端身份与 Git 基线

- 主机：`y9000p-ai`
- 用户：`james`
- 仓库：`/home/james/complex-management`
- 阶段分支：`audit/prd-phase1-baseline`
- 起始提交：`599dc9e8a9eb56f70dde7f7629e1087d04b76a8c`
- 起始工作区：干净
- 起始 `main == origin/main`

## 权威 HTML 哈希

| 文件 | SHA-256 |
|---|---|
| `business-system-map-for-operations-v0.1.html` | `0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e` |
| `platform-governance-map-for-operations-v0.1.html` | `a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b` |
| `business-enum-quick-reference-v0.1.html` | `3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648` |

## 范围结果

- 最终解释 HTML：3 份。
- 现行 PRD及相关文档：30 份。
- 纳入清单总计：33 个实际文件。
- 排除范围：历史归档、开发设计、实现计划、OpenAPI、代码与数据库、测试和运行证据、运维文档。

详细清单及每个文件的基线哈希见 `../document-scope-manifest.csv`。

## 历史续接结果

- 备份登记快照：38 份。
- 与当前仓库哈希一致：37 份。
- 已变化：1 份，即业务枚举 HTML。
- 历史裁决主题：29 节。
- 可作为后续审计输入：前 28 节。
- 仍含未裁决事项：第 29 节积分清算。

详细差异见 `../backup-current-diff.md`，迁移后的完整裁决原文见 `../business-decision-ledger.md`。

## 基线验证

- 阶段 worktree 已执行独立 `npm ci`。
- `node_modules` 是真实目录，不是主工作区依赖链接。
- `npm run verify:predeploy`：通过。
- 依赖安装报告现有 `1` 个高危依赖项；本阶段不修改依赖。

## 阶段完成门

阶段只有在以下条件全部满足后完成：

- 本阶段文件通过结构、范围和引用验证；
- `git diff --check` 通过；
- 变更范围仅限 `docs/prd-audit/`；
- 本地和 GitHub `predeploy` 通过；
- 阶段 PR 合并；
- 远端 `main == origin/main` 且主工作区干净。
