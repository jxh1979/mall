# 160 编码保护基线 V0.1

## 1. 强制结论

本项目正式编码、测试、构建、安全扫描和提交的唯一主仓在 160 机器上：

- canonical_host_ip=192.168.88.160
- canonical_hostname=y9000p-ai
- canonical_repo_root=/home/james/complex-management
- local_role=coordination_quality_only
- local_code_policy=no_formal_code_on_local_machine

本机只负责进度检查、任务拆解和质量检查，不得在本机仓库提交正式代码，不得把本机 OneDrive 文档目录或临时目录当成正式代码仓。

## 2. 为什么要设保护基线

之前出现过本机侧看到代码痕迹的风险。该风险不是业务功能缺陷，但会直接影响交付可信度：

- 开发者可能在错误机器上写代码，导致 160 不是最新实现。
- 测试可能在错误环境上运行，掩盖 Java 21、Android SDK、PostgreSQL、NAS、Semgrep 等真实前置条件。
- 文档、审计和提交证据可能来自不同机器，导致生产前置证据不可追溯。

所以从本基线起，正式编码证据只认 160 仓库和 160 验证结果。

## 3. 分工边界

| 对象 | 允许 | 禁止 |
|---|---|---|
| 160 仓库 | 正式编码、测试、构建、安全扫描、提交、生产前置证据回填 | 绕过验证直接提交 |
| 本机 Codex | 进度检查、任务拆解、质量检查、远程调度、读取文档 | 新建或提交正式代码、在本机跑正式通过结论 |
| 本机 OneDrive 文档目录 | 保存产品文档、审计报告和交付文档 | 成为 Java/Spring/前端/Android 正式代码仓 |
| 第三方供应商 | 按交付包读取 OpenAPI、Runbook、验收矩阵 | 以本机路径、历史原型或旧脚本作为正式实现依据 |

## 4. 机器验证规则

新增 `npm run verify:coding-host-protection`，用于检查：

1. 当前执行主机必须是 `192.168.88.160` / `y9000p-ai`。
2. 当前 git root 必须是 `/home/james/complex-management`。
3. `package.json` 必须暴露 `verify:coding-host-protection`。
4. `verify:ops160-recovery` 必须包含 `verify:coding-host-protection`。
5. `verify:p0` 必须包含 `verify:coding-host-protection`，防止完整 P0 验证在非 160 机器上被误判通过。
6. 本文档和 `docs/production-readiness-evidence/coding-host-protection-latest.json` 必须存在并与当前主机边界一致。

## 5. 本机污染处理规则

如果本机出现正式代码文件或 Git 仓库：

1. 先暂停本机代码写入。
2. 对比 160 的提交号和差异，确认是否有未同步的有效改动。
3. 有效改动必须由人工审查后迁移到 160，不得直接把本机仓库作为主仓。
4. 迁移后清理本机代码痕迹，只保留文档和远程调度能力。
5. 在 160 重新运行 `npm run verify:coding-host-protection`、`npm run verify:p0-contract`、`npm run test:backend`、`npm run build` 和必要安全扫描。

## 6. 验收标准

### AC-CODEHOST-001 主机唯一

Given 任意人员准备执行正式编码或完整 P0 验证，
When 执行 `npm run verify:coding-host-protection`，
Then 只有 160 的 `/home/james/complex-management` 仓库可以通过。

### AC-CODEHOST-002 本机只协调

Given 本机 Codex 参与任务，
When 需要修改代码、运行 Java/Android/安全扫描或提交，
Then 必须通过 SSH 在 160 上执行；本机只输出计划、拆解、审查和进度检查。

### AC-CODEHOST-003 完整 P0 防误跑

Given 有人尝试在非 160 机器上执行完整 `npm run verify:p0`，
When `verify:p0` 触发保护闸口，
Then 验证必须失败，避免把本机环境结果当作 160 生产前置证据。

## 7. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-15 | V0.1 | 建立 160 编码保护基线，明确本机只负责进度检查、任务拆解和质量检查；新增 `verify:coding-host-protection` 并接入 `verify:ops160-recovery` 与 `verify:p0`。 |