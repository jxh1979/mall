# phase3 交叉复核发现（附录轨 vs 已审治理主题）

基线：`main@b4290638`
扫描 APPENDIX 文本文件：140（优先 openapi/governance/permission/evidence/ops/design，单文件截断 400KB）
phase3 覆盖行：302
规则账本：844

## 总表

| ID | 主题 | 判定 | 严重度 | 账本命中 | phase3覆盖 | 附录支持命中 | 冲突词命中 |
|---|---|---|---|---:|---:|---:|---:|
| CC-GOV-DENY-PRIORITY | 显式拒绝优先 | ALIGNED_OR_SUPPORTED | P3 | 12 | 7 | 51 | 0 |
| CC-GOV-TEMP-AUTH | 临时授权到期/单次 | ALIGNED_OR_SUPPORTED | P3 | 13 | 7 | 73 | 0 |
| CC-GOV-SELF-APPROVE | 禁止自审自批 | ALIGNED_OR_SUPPORTED | P3 | 2 | 1 | 10 | 0 |
| CC-GOV-HIGH-RISK-AUTH | 高危授权先审后生效 | DOC_RULE_DECIDED_CODE_PENDING | P1 | 1 | 1 | 84 | 1 |
| CC-GOV-OUTSOURCE-SCOPE | 外包最小够用 | ALIGNED_OR_SUPPORTED | P3 | 6 | 4 | 94 | 0 |
| CC-GOV-AUDITOR-RO | 审计只读+敏感留痕 | ALIGNED_OR_SUPPORTED | P3 | 7 | 3 | 86 | 0 |
| CC-GOV-ORG-CHANGE-RESIDUAL | 组织变化权限残留 | ALIGNED_OR_SUPPORTED | P3 | 8 | 5 | 52 | 0 |
| CC-GOV-TERMINAL-SWITCH | 终端关闭后端拒绝 | ALIGNED_OR_SUPPORTED | P3 | 4 | 4 | 102 | 0 |
| CC-GOV-TEN-TERMINALS | 十类终端拆分 | ALIGNED_OR_SUPPORTED | P3 | 38 | 20 | 38 | 0 |
| CC-GOV-FOUR-SUBJECT-BIND | 四类主体绑定 | ALIGNED_OR_SUPPORTED | P3 | 3 | 3 | 87 | 0 |
| CC-GOV-DATA-ISOLATION | 商户/项目隔离 | ALIGNED_OR_SUPPORTED | P3 | 13 | 10 | 63 | 0 |
| CC-GOV-EVIDENCE-TRACE | 操作留痕/证据 | NEEDS_DOMAIN_PHASE_COVERAGE | P2 | 65 | 28 | 131 | 0 |

> 自动化初判曾把 HIGH-RISK 标为 `POTENTIAL_REVERSE_CONFLICT`。人工核摘录后改判为 **附录在记录已知实现漂移**，并非 APPENDIX 试图推翻 HTML。

## P1 详述：高危/高价值动作“先生效后审批”

- 权威规则：`PGM-0089`（平台治理 HTML）要求高危授权/高危业务**先审批再生效**。
- phase3 覆盖状态：`FULL`。
- 附录命中：`docs/dev-plan-v7-audit-20260722.md:63`
  - 记录酒吧高价值退酒存在“先生效后审批”（FLOW-07 / `BarOrderService`）
  - 建议改为：阈值配置化 + 高价值先审后行 + 双证据
- 解释：
  - 这是 APPENDIX 审计报告对实现漂移的指认，方向与 HTML 一致。
  - **不构成**“设计/OpenAPI 反向立法”。
  - 说明治理 PRD 主轨 FULL 之后，实现/历史审计附录仍可能暴露残余风险。
- 建议处置：
  1. 保持 `PGM-0089` 与 phase3 FULL 不变
  2. 在附录轨登记为 `RESIDUAL_IMPLEMENTATION_DRIFT`
  3. 代码修复不在本轮文档审计完成线内

## P2 详述：留痕/证据主题覆盖不完整

- 账本中大量规则含“留痕/证据”措辞（65）
- phase3 仅覆盖其中 28 条（FULL 26 + NA 2）
- 附录大量提到 evidence/audit
- 解释：多数属于业务域证据规则，应在 phase4–6 主轨覆盖，不是 phase3 被 OpenAPI 打脸。
- 建议：phase4 起按域收敛 evidence 强制对抗场景，降低关键词假阳性。

## OpenAPI 存在性抽检（非逐 operation 审计）

| 检查 | 结果 |
|---|---|
| evidence/events 或 audit | 出现 |
| merchant/project 范围 | 出现 |
| permission/role/scope | 出现 |
| terminal/device | 出现 |
| approval/workflow | 出现 |

结论：OpenAPI 对治理关键面有名词级承接迹象；按 B+3 不在本轮做全量 operation 逐条审计。

## 未发现

- 未发现 APPENDIX 系统性主张“职级覆盖显式拒绝 / 可自批 / 外包默认可看全局 / 审计可改业务 / 九类终端”等与 phase3 已修口径硬对着干的成体系反向立法。
- 唯一高优先级附录信号是实现漂移被审计文档记录，不是文档权威层冲突。

### 更新（2026-07-27）文档收口

- 用户确认方案 **A1**：仅已出品；默认阈值超过 **5000 元**可配；高价值先审后行+双证据；低价值可即时生效。
- 写入：`bar-business-operations-product-prd-v0.1.md` §10.2.1、`business-decision-log` `BAR-VOID-A1`。
- 状态改为 **DOC_RULE_DECIDED_CODE_PENDING**（本轮不动编码）。
- `PGM-0089` phase3 FULL 保持不回滚。
