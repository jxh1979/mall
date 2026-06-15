# 2026-06-15 T3-A 只读经营分析实施计划

机器标记：T3A_READONLY_ANALYTICS_PLAN_READY
机器标记：allowed_scope=readonly_analytics_only
机器标记：no_backend_write_api_change
机器标记：no_python_ai_service
机器标记：no_real_vendor_control

## 目标

在 T3-0 编码包护栏下，完成 T3-A 只读经营分析的最小可编码切片。该切片只用于总控和运营经理查看集团/项目/商户经营趋势、商户健康、员工服务、客户意图、机器人经营指标和指标口径，不产生任何生产控制动作。

## 修改文件

- 修改 `apps/web/src/main.tsx`：修正 analytics DTO 类型，新增 T3-A 只读经营分析视图，复用现有 analytics GET 接口。
- 修改 `package.json`：新增 `verify:t3a-readonly-analytics`。
- 新增 `tools/verify-t3a-readonly-analytics.mjs`：机器校验 T3-A 页面边界。
- 修改 `docs/project-progress-dashboard.md`：记录 T3-A 当前交付状态。
- 新增本文档作为实施计划。

## 新增文件

- `tools/verify-t3a-readonly-analytics.mjs`
- `docs/superpowers/plans/2026-06-15-t3a-readonly-analytics.md`

## 数据结构/接口变化

- 不新增数据库表。
- 不新增 OpenAPI 路径。
- 不新增后端 Controller、Service、Repository 写接口。
- Web 端仅修正对现有 `/api/v1/analytics/*` GET 返回字段的类型和展示口径。

## 测试点

1. RED：页面未包含 T3-A 只读标记、类型修正和只读视图区时，`verify:t3a-readonly-analytics` 必须失败。
2. GREEN：补齐 T3-A 只读视图后，`verify:t3a-readonly-analytics` 必须通过。
3. 类型检查：`npm run typecheck --workspace apps/web` 必须通过。
4. 构建：`npm run build` 必须通过。
5. 回归：`npm run verify:p0-contract`、`npm run test:backend`、`npm run verify:security-baseline` 必须通过。
6. 安全：Semgrep 不得出现新增 findings。

## 验收标准

- 页面入口显示 `T3-A 只读经营分析`。
- 页面只读展示现有 analytics GET 数据，不出现创建、处理、发布、审批、支付、核销、调价、库存扣减等动作按钮。
- 页面展示指标来源、时间窗或口径说明。
- 机器人、AI、客户意图、员工服务评分和商户健康均为摘要/解释，不触发真实供应商控制或 AI 生产决策。
- 所有验证在 160 机器执行。

## 风险和回滚

风险：现有 Web 单文件较大，修改可能引入类型或构建失败。回滚方式是撤回本次 Web 视图和校验脚本变更，不影响后端、数据库和 OpenAPI。
