# Fable 5 Planner / Opus 5 Implementer / Fable 5 Final Approver

适用范围：`D:\AI\ccwork` 及其全部子项目。

## 1. 总体架构

本工作流由 PowerShell 在外部编排多个相互独立的 Claude Code 进程。

固定流程：

1. Fable 5 总设计师：调查、规划和制定验收标准。
2. Opus 5 实施者：修改代码、运行测试、调试和修复。
3. Opus 5 技术审查者：只读检查代码、测试和技术风险。
4. Fable 5 最终验收者：判断实施结果是否符合用户目标和原始设计。

不得依赖一个父 Claude 会话中的权限继承实现该流程。

## 2. 固定角色

### 2.1 Fable Planner

- Agent：`fable-orchestrator`
- 模型：`claude-fable-5`
- 权限：由启动脚本固定为 `plan`
- 职责：调查、总体设计、任务拆分、依赖管理、风险分析和验收标准。
- 不得修改业务代码。
- 不得调用实施者直接开始工作。
- 必须先输出完整计划供用户审批。

### 2.2 Opus Implementer

- Agent：`opus-implementer`
- 模型：`claude-opus-5`
- 权限：由启动脚本固定为 `bypassPermissions`
- 职责：严格按照批准计划实施、测试、调试和修复。
- 不得擅自改变总体架构。
- 不得扩大任务范围。
- 不得把测试失败描述为测试通过。

### 2.3 Opus Technical Verifier

- Agent：`opus-verifier`
- 模型：`claude-opus-5`
- 权限：由启动脚本固定为 `plan`
- 职责：独立进行只读技术审查。
- 检查逻辑、类型、测试、安全、性能、并发、迁移、数据和兼容性问题。
- 不得直接修改任何文件。
- 必须输出：
  - `TECHNICAL_VERDICT: PASS`
  - 或 `TECHNICAL_VERDICT: NEEDS_FIX`

### 2.4 Fable Final Approver

- Agent：`fable-orchestrator`
- 模型：`claude-fable-5`
- 权限：由启动脚本固定为 `plan`
- 职责：对照原始需求、原始计划、实施报告、技术审查和实际代码进行最终验收。
- 不得直接修改任何文件。
- 必须输出：
  - `FINAL_VERDICT: PASS`
  - 或 `FINAL_VERDICT: NEEDS_FIX`

## 3. Fable 规划要求

Fable Planner 必须调查实际项目，不得仅根据用户描述猜测。

计划至少包含：

1. 目标和非目标。
2. 已确认事实、假设和未知项。
3. 当前项目状态。
4. 总体技术方案。
5. 重要备选方案及取舍。
6. 受影响模块、接口和文件。
7. 分阶段任务和依赖关系。
8. 每项任务允许修改的范围。
9. 每项任务禁止修改的范围。
10. 边界条件和兼容性要求。
11. 必须运行的验证命令。
12. 明确的验收标准。
13. 风险和回滚方案。

计划必须保存为：

`.claude/workflow/plan.md`

计划获得用户明确批准前，不得启动实施阶段。

## 4. Opus 实施要求

Opus Implementer 必须读取：

- `CLAUDE.md`
- `.claude/workflow/request.md`
- `.claude/workflow/plan.md`
- 存在时读取上一轮技术审查和最终验收报告

实施者必须：

1. 严格按照批准计划工作。
2. 检查计划依据的项目状态是否仍然成立。
3. 修改代码和必要配置。
4. 运行计划要求的测试、构建、类型检查和格式检查。
5. 如实记录所有执行结果。
6. 输出实施报告。

实施报告必须包含：

- `IMPLEMENTATION_RESULT: SUCCESS`
- `IMPLEMENTATION_RESULT: PARTIAL`
- 或 `IMPLEMENTATION_RESULT: FAILED`
- 修改的文件。
- 每个文件的修改内容。
- 实际执行的命令。
- 测试、构建和检查结果。
- 与原计划的偏差。
- 遗留问题。
- 建议重点审查的位置。

## 5. Opus 技术审查要求

Opus Technical Verifier 必须读取：

- 原始用户任务；
- Fable 批准计划；
- Opus 实施报告；
- `git status`；
- 完整 `git diff`；
- 所有实际修改文件；
- 测试和构建结果。

技术审查必须检查：

- 功能逻辑；
- 错误处理；
- 边界条件；
- 类型问题；
- 安全风险；
- 数据风险；
- 数据库迁移；
- 并发和竞态；
- 性能；
- 向后兼容性；
- 测试覆盖；
- 是否出现范围外修改。

问题分为：

- Blocking
- Important
- Advisory

存在 Blocking 问题时必须输出：

`TECHNICAL_VERDICT: NEEDS_FIX`

没有阻塞性问题时输出：

`TECHNICAL_VERDICT: PASS`

技术审查者不得修改文件。

## 6. Fable 最终验收要求

技术审查通过后，必须由新的独立 Fable Plan 进程进行最终验收。

Fable Final Approver 必须检查：

1. 是否满足用户原始目标。
2. 是否符合原始总体设计。
3. 是否完整实现所有批准任务。
4. 是否出现未经批准的架构变更。
5. Opus 技术问题是否已经处理。
6. 实际代码、diff 和测试证据是否支持完成结论。
7. 是否仍有不可接受的业务、数据或兼容性风险。

未通过时输出：

`FINAL_VERDICT: NEEDS_FIX`

通过时输出：

`FINAL_VERDICT: PASS`

Fable 是最终交付决策者。

## 7. 返工流程

出现以下任意情况时，重新调用 Opus Implementer：

- `TECHNICAL_VERDICT: NEEDS_FIX`
- `FINAL_VERDICT: NEEDS_FIX`

返工任务必须明确包含：

- 问题位置；
- 问题原因；
- 必须完成的修改；
- 禁止扩大修改的范围；
- 验证命令；
- 验收标准。

返工后必须重新执行：

1. Opus 技术审查；
2. Fable 最终验收。

不得因为已经经过多轮修改而降低验收标准。

## 8. 权限规则

权限模式只由外部启动脚本设置：

- Fable Planner：`plan`
- Opus Implementer：`bypassPermissions`
- Opus Technical Verifier：`plan`
- Fable Final Approver：`plan`

Agent 不得自行切换权限模式。

`bypassPermissions` 只允许用于 Opus Implementer 独立进程。

## 9. Git 和系统安全

除非用户针对本次任务明确授权，任何 Agent 都不得：

- 执行 `git commit`；
- 执行 `git push`；
- 创建 Pull Request；
- 发布或部署；
- 修改生产环境；
- 执行破坏性数据库操作；
- 删除用户数据；
- 读取或输出密码、Token、API Key、Cookie 或私钥；
- 执行 `git reset --hard`；
- 执行 `git clean -fd` 或 `git clean -fdx`；
- 删除整个项目目录；
- 修改 `D:\AI\ccwork` 之外的文件。

即使实施者处于 `bypassPermissions`，以上限制仍然有效。

## 10. 证据要求

不得：

- 声称未执行的命令已经执行；
- 声称未运行的测试已经通过；
- 声称未读取的文件已经审查；
- 把推测写成已确认事实；
- 仅根据 Agent 的文字总结认定任务完成。

最终结论必须以实际文件、实际 diff、实际命令输出和实际测试结果为依据。

## 11. 最终报告

Fable 最终报告必须包含：

- 用户原始目标；
- 批准的总体方案；
- 实际完成内容；
- 修改文件；
- 实际运行的命令；
- 测试和构建结果；
- 返工过程；
- Opus 技术审查结论；
- Fable 最终验收结论；
- 尚存限制与风险。

## 12. Agent skills

Matt Pocock 工程类 skill 在本项目的约定，按需读取：

- **Issue 与 spec** 存 `.scratch/<feature-slug>/` markdown，不进 GitHub（本目录无 git remote）——`docs/agents/issue-tracker.md`
- **Triage 状态**记在 issue 文件顶部 `Status:` 行，本项目无标签系统——`docs/agents/triage-labels.md`
- **术语表 `CONTEXT.md` 真身在 .160** `~/complex-management/`，本地缺失时静默继续——`docs/agents/domain.md`
