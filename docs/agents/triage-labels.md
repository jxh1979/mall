# Triage Labels

这些 skill 用五个规范化的 triage 角色说话。本文把这些角色映射到本仓库 issue tracker 实际使用的标签串。

| Label in mattpocock/skills | Label in our tracker | 含义                      | 仓库现状     |
| -------------------------- | -------------------- | ------------------------- | ------------ |
| `needs-triage`             | `needs-triage`       | 需要维护者评估            | 首次使用时建 |
| `needs-info`               | `needs-info`         | 等报告人补充信息          | 首次使用时建 |
| `ready-for-agent`          | `ready-for-agent`    | 已完整定义，可交 AFK 代理 | 首次使用时建 |
| `ready-for-human`          | `ready-for-human`    | 需要人来实现              | 首次使用时建 |
| `wontfix`                  | `wontfix`            | 不予处理                  | **已存在**   |

skill 提到某个角色时（如 "apply the AFK-ready triage label"），用本表右列对应的标签串。

`wontfix` 是 GitHub 出厂标签，本仓库已有（描述 "This will not be worked on"）—— 直接复用，不要新建重复标签。
其余四个仓库尚无，`/triage` 首次用到时建即可（`gh label create <name>`）。

右列可随时改成你实际使用的词表。
