# 2026-06-15 160 编码保护基线实施计划

## 目标

把“代码只在 160、本机只负责进度检查、任务拆解和质量检查”从口头规则变成可验证的仓库基线。

## 修改文件

- 新增 `tools/verify-coding-host-protection.mjs`
- 新增 `docs/coding-host-protection-baseline-v0.1.md`
- 新增 `docs/production-readiness-evidence/coding-host-protection-latest.json`
- 修改 `package.json`
- 视需要同步 `docs/p0-coding-entry-pack-v0.1.md`、`docs/project-progress-dashboard.md`

## 数据结构和接口变化

无业务数据库和 OpenAPI 变化。新增生产前置证据 JSON：

- `status`
- `canonical_host_ip`
- `canonical_hostname`
- `canonical_repo_root`
- `local_role`
- `local_code_policy`
- `guarded_scripts`
- `verification_commands`

## 测试点

1. RED：缺保护文档和证据时，`npm run verify:coding-host-protection` 必须失败。
2. GREEN：补齐文档、证据和 package 闸口后，保护验证必须通过。
3. 回归：`verify:ops160-recovery` 和 `verify:p0` 必须包含保护验证。
4. 质量：`git diff --check`、P0 契约、后端测试、Web build 和安全基线通过。

## 验收标准

- 160 上 `npm run verify:coding-host-protection` 输出 `CODING_HOST_PROTECTION_PASS`。
- 非 160 主机默认无法通过完整 P0 验证。
- 文档明确本机只负责协调和质量检查，不得承载正式代码。

## 风险和回滚

风险：未来 CI 或新预生产机器需要运行完整 P0 时会被保护闸口阻断。

回滚方式：通过环境变量 `COMPLEX_EXPECTED_CODING_HOST_IP`、`COMPLEX_EXPECTED_CODING_HOSTNAME`、`COMPLEX_EXPECTED_CODING_REPO_ROOT` 明确迁移后的受控主机；不得直接删除保护脚本。