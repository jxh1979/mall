# 审计执行说明

版本：V0.02

本次唯一审核标准为操作员指定的 `160-audit-prompt.md V0.17`。本轮复核模块归属，同时审查操作员明确纳入的未发布员工材料；历史 87 模块合同及 Claude/Fable 流程只作历史记录。

## 1. 固定边界

- 正本与唯一写入工作区：`/home/james/complex-management` 的 `main` 主工作区。
- 纳入范围只认 `plan/scope.txt`；排除项只按提示词 §1.2 的路径前缀判断。
- 三份业务 HTML 是唯一业务需求出发点，纳入审计但全程只读；审出其自身问题时按铁律 4 暂停并逐条申请。
- 阶段 B 不提交、不推送、不创建 worktree。当前专项提示词对本轮审计落点的明确规定优先于仓库通用 worktree 默认规则。
- 只允许写提示词 §7 列明的审计产物，以及 B3 中已有定稿 finding ID 可逐行归因的修复与最小验证。

## 2. 技能预检与节点使用

Codex 已在开工前按实际目录核验：

- superpowers：`using-superpowers`、`brainstorming`、`writing-plans`、`systematic-debugging`、`test-driven-development`、`requesting-code-review`、`receiving-code-review`、`verification-before-completion` 均存在于 `C:\Users\jxh1\.codex\plugins\cache\claude-plugins-official\superpowers\6.3.0\skills\`。
- mattpocock：`grilling`、`grill-me`、`codebase-design`、`diagnosing-bugs`、`tdd`、`resolving-merge-conflicts`、`writing-for-agents` 均存在于 `C:\Users\jxh1\.agents\skills\`；可选的 `git-guardrails-claude-code`、`batch-grill-me`、`ubiquitous-language` 也存在。
- ponytail：`ponytail` 与 `ponytail-review` 均存在于 `C:\Users\jxh1\.codex\plugins\cache\ponytail\ponytail\4.9.0\skills\`；`i-have-adhd` 与 `ponytail` 输出/实现模式由当前会话规则启用。
- 首次使用对应技能时，Codex 先读取其实际 `SKILL.md` 全文，再按提示词 §6 的收窄口径使用。Grok 在方案复核时独立自检技能并把缺口写进结论；不得用未核实技能名充数。

明确不用的技能及原因以提示词 §6 为唯一说明，不在本文件另造替代流程。

## 3. 每模块三条审计线

### 3.1 第一性原理

逐文件、逐接口追问并落证据：

1. 为什么必须存在；删除后哪条 HTML 做不到。答不上不直接删除，继续走四分类。
2. 声称的行为是否实际执行；记录命令、exit code、关键输出或不能执行的产物类型豁免。
3. 哪个前提未经验证；用 HTML 原文、可执行产物、外部规范原文或可复现命令验证。

### 3.2 顺从式

先假定实现意图正确，再逐一核对本模块映射的全部判据 ID：输入、状态、输出、错误、权限、持久化、测试和文档是否共同实现同一口径。每个文件都必须有覆盖记录；无 finding 不等于未审。

### 3.3 对抗式

先假定实现错误，使用本席已核实可调用的 `grilling` / `grill-me` / `batch-grill-me` 追问结构（缺失项记缺口，不冒充调用），自行构造最小反例：越权、重复、并发、边界值、失败重试、状态跳转、金额精度、配置漂移、跨层枚举不一致、下线/删除绕过和文档假完成。事实由当前席自行读取，不派第三席、不等操作员。

## 4. 四分类与 finding 证据

按固定顺序判定：冲突 → 细化 → 技术性 → 添加。

每条临时 finding 必含：席位临时编号、`文件:行号`、复现命令及输出或 HTML 原文、2.1 分类、2.3 依据类型、引用的冻结判据 ID、工程缺陷是否成立。技术性项另写“无业务发明”的理由；添加项在 B1/B4/B5 的整批完整报告收齐后进入 B2，再按提示词 2.2 写 `questions/<ID>.md` 并等待实际答复；不在审计中途停止去修复。

只有 `criteria-lock.md` 中 HTML 派生 ID 或 `general` ID 可支撑 finding。锁外主张不进队列，必须走 `LOCK-AMEND` 五步。

## 5. 按产物类型取证

- 可执行代码：优先最窄构建、单测或直接调用；关键分支需有可失败断言。涉及数据库时仅导出三个 `COMPLEX_DATABASE_*` 变量并使用 `complex_test`。
- 文档与说明性 Markdown：不强制造运行测试；用 HTML 逐字对照、链接/路径存在性、命令实跑和结构解析证明。
- SQL/迁移：用目标 PostgreSQL 的解析、迁移 dry-run 或隔离测试库验证；纯 SQL 内容修复不强制额外测试文件。
- 配置、CI、脚本与 OpenAPI：先用原生解析器/现有仓库门禁，再在可丢弃或无副作用上下文运行真实入口；不得部署生产环境。
- `OVERSIZE-SINGLE-FILE`：允许按结构段分批阅读和执行检查，但 raw 必须列出全文件覆盖表，不能只抽样后宣称整文件 PASS。

只有 Grok 交付失败才原样重跑同一条调用命令，并累计“子代理失败重跑次数”；适配器故障先修适配器。普通验证命令失败记录真实原因、exit code 与输出，不混入 Grok 重跑次数，也不折算为业务 finding 或 PASS。

## 6. Grok 无头调用

沿用已核实的既有参数形态：

```bash
/home/james/.local/bin/grok -p "<本步骤完整审计指令>" -m grok-4.6 --reasoning-effort xhigh --output-format json --permission-mode bypassPermissions --no-subagents --disable-web-search --cwd /home/james/complex-management
```

Grok 角色只读业务代码与计划；除提示词指定的 `*-grok-raw.md` 外不得写文件。A2/LOCK-AMEND 只把结论返回 stdout，不新增文件。每次必须独立实算哈希并输出独占一行：

`CRITERIA-LOCK-SHA256: <64 位小写 hex>`

A2/LOCK-AMEND 直接读取 CLI JSON，操作员回执附结论全文、独算哈希及 exit code；原始 JSON 不落审计产物。B1/B2/B4/B5 经提示词 §0 的适配器只返回指定报告路径与真实 exit code，原始 stdout/stderr 只在进程内缓冲丢弃。B1/B4/B5 的 Grok 正文只在双方本次完整 raw 落盘后读取，Codex self raw 第一行同样写独算哈希。Grok 运行期间每 5 分钟检查一次交付，不设运行时长上限。

## 7. 双席独立落盘

B1、B4、B5 固定顺序：每席必须把本次全部范围审完、全部问题收齐再结束本阶段；两席本次完整报告落齐后才统一判断，禁止发现一条就停下来修复。

1. 先启动 Grok；要求写指定 grok raw，只向 Codex 暴露适配后的路径与 exit code。
2. Grok 完成后，Codex 在不读取该 raw 的前提下独立审计并写 self raw。
3. 两份都存在且非空后，Codex 才读取 Grok raw并做合取判定。

B2 候选台账由 Codex 先写，Grok 对两份触发 raw 与候选台账做差集；两席分别把 B2 PASS/FAIL 及本批输入 raw 路径追加到自己的 B1 raw；B2 追加段不得写 `CRITERIA-LOCK-SHA256:` 行。任一席 FAIL 都不得定稿或前进。

## 8. B1–B5 与回退

- B1：首次把模块从 `PENDING` 改为 `IN_PROGRESS`，两席三线全审；B1 后做四值哈希校验。
- B2：分配永久 ID、完成四分类与修复队列归属；有添加项立即 `BLOCKED-ASK`。修复队列为空时跳过 B3/B4，仍须 B5。
- B3：只修当前仍归属“是”的定稿 ID；可执行行为变更先写归属该 ID 的最小失败测试；文档、配置、纯 SQL、HTML 内容修复不强制造测试，采用提示词 2.3 类型 4 的最小命令举证；再作最小修复。B3 前后各冻结 tree id、全量 binary diff并做四值校验。
- B4：两席只审 before/after tree diff，每块逐行绑定 finding ID；任一无 ID 行使整轮 FAIL 并回滚。
- B5：两席重审整个模块。双 PASS 且 B5 后四值全等时，先把 ledger 状态改 `PASS`，再写模块收口文件。

B4/B5 收齐本次整批两席意见后统一路由：只要有一条没有定稿 ID 或不在修复队列，整批先回 B2；全部已有 ID 且仍属修复队列时才整批回 B3，禁止拆批先修。每模块最多执行 B3 三次；第 3 次后仍完成对应 B4/B5，仍 FAIL 则记录 `BLOCKED-3ROUNDS` 并停止，等待操作员新指令。

B3 前后树 ID 使用独立临时索引：`GIT_INDEX_FILE="$(mktemp -d)/index"`，在同一环境下执行 `git add -A && git write-tree`，不得把 mktemp 已建空文件当索引。临时索引不替换主索引；完整 binary diff 按两树取得。

B4/B5 文件名包含 B3 轮次；跳过 B3 时 B5 使用 r0；同轮重新审查追加本次完整报告，不覆盖旧报告。阶段 A 初始方案修复上限 3，第三次修复后仍未双 PASS 则记 `BLOCKED-PLAN-ROUNDS` 并停止；阶段 B 触发的 LOCK-AMEND 按提示词 §5 单独累计，不能混用这两个既有计数。

## 9. PASS / FAIL 口径

单席 PASS 必须同时满足：本模块所有文件有覆盖记录；映射的所有判据 ID 已核对；三线均执行；所有声称均有四类证据之一；无未答 questions；无未定稿缺陷；Grok 哈希行有效。

任一缺口、未答问题、未验证前提、无效报告载荷或另一席 FAIL，步骤结论即 FAIL 或生命周期停在相应 `BLOCKED-*`，不存在“有条件通过”。

## 10. 判据锁与收口

B1 后、B5 后及每轮 B3 前后，按提示词 §5 闸 1 比较实算、sidecar、ledger、当前精确 Grok raw 四值并把四值写进操作员回执。任意不等立即 `BLOCKED-GLOBAL-CLOSE`，不得自行重算对齐。

全部模块 PASS 后复跑三条 `comm`、四值哈希、ledger 全绿、findings 无待修、questions 全部精确答复；任一失败即 `BLOCKED-GLOBAL-CLOSE`。

## 11. 本轮计划恢复与未发布输入

正式 main 模块与旧编号的完整文件交叉索引见 `plan/modules.md`。模块号必须连同计划、名称和文件列表识别；不能按同号继承旧 87 模块的 PASS。未发布员工材料按同文件末尾分段表完整审查，版本和路径须写入对应原始报告，不能把这些输入副本冒称 main 源码或已发布修复。

本次冻结摘录与映射修订严格走提示词 §5 闸 2 的 LOCK-AMEND：先在 ledger 登记全部变更 ID 并保留旧哈希；修改定义与映射、生成 sidecar；两席复核完整 diff，Grok 反向应用 diff 的旧副本必须与 ledger 旧基准相等；双 PASS 后才更新 ledger 基准及计数，并按原规则重开最早受影响模块。阶段 A 的历史方案修复次数保留，不用于替代 LOCK-AMEND 计数。
