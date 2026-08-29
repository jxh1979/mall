# 2026-06-15 T3-0 编码包实施计划

## 目标

在不直接进入 T3 功能编码的前提下，建立 T3-0 编码包和机器校验闸口，防止高级分析、供应商控制和独立 AI 服务混线实施。

## 修改文件

- 新增 `tools/verify-t3-coding-package.mjs`
- 新增 `docs/t3-coding-entry-pack-v0.1.md`
- 新增 `docs/t3-user-story-map-and-sprint-slices-v0.1.md`
- 新增 `docs/t3-security-and-supplier-readiness-gate-v0.1.md`
- 新增 `docs/t3-entry-assessment-and-coding-package-audit-v0.1.md`
- 修改 `package.json`
- 修改 `docs/00-current-effective-document-entry-v0.1.md`
- 修改 `docs/project-progress-dashboard.md`

## 数据结构/接口变化

无业务数据库、OpenAPI、后端 Controller、Web 页面和 Android 代码变化。

## 测试点

1. RED：缺 T3 文档和入口引用时，`verify:t3-coding-package` 必须失败。
2. GREEN：补齐 T3-0 文档后，`verify:t3-coding-package` 必须通过。
3. 回归：P0 契约、后端测试、Web build、安全基线和 Semgrep 保持通过。

## 验收标准

- T3-0 文档明确 T3-A 可作为下一步。
- T3-C/T3-D 在供应商和 AI 治理资料到位前被阻断。
- T3-0 包有机器校验命令。

## 风险和回滚

风险：T3 包过严会阻断后续真实供应商联调。回滚方式不是删除闸口，而是在供应商资料到位后更新就绪矩阵、验收证据和对应 T3 切片开发计划。
