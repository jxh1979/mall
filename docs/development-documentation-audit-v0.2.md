# 开发文档完整性审计 V0.2（对当前代码态刷新）

> 注（2026-06-18）：完成审计/闭环报告存在多份重复。当前态以 R1~R4 交付物（`r1-*-20260618.md`、`r2-code-audit-req26b-req21-req30-20260618.md`、`r3-*-20260618.md`、`r4-remaining-work-backlog-20260618.md`）+ 本 v0.2 + `closed-loop-live-acceptance-audit-20260618.md` 为准，其余完成审计/闭环报告降级为历史参考（不删除）。当前事实基线：209 ops / 729 测试 / Flyway V060。
> 审计日期：2026-06-18
> 方法：documentation-specialist + PM critic + acceptance/edge-cases + user-story-map + gstack spec/review，3 路并行只读审计 + 实测代码事实对照。
> 取代关系：本审计刷新 `development-documentation-adversarial-audit-v0.1.md`（2026-06-16，已对当前代码滞后：写 198 ops/V041，实际 207 ops/V052）。v0.1 降级为历史。
> 边界：本轮仅改文档，不改代码。

机器识别标记：DOC_AUDIT_V0_2_READY、SUPERSEDES_ADVERSARIAL_AUDIT_V0_1、CODE_FACTS_AS_OF_20260618。

## 1. 当前代码事实（本审计基准，均实测）
- OpenAPI：**170 paths / 207 operations**（`docs/openapi/openapi-p0.json`）。
- 数据库迁移：**52 个（V001–V052）**；V044–V052 多为租户隔离/安全加固。
- 后端测试：**673 / 0 失败 / 0 错误 / BUILD SUCCESS**。
- 前端：Playwright UI **28**；真后端 seeded UI **2**；web 单测 16；完整后台页面 **30**。
- `verify:delivery-gates`：**exit 0**；Semgrep：results=0。
- 结论：可开发/联调/业务验收；**不可宣称生产上线**（外部证据待回填）。

## 2. 文档地图（当前态）
docs/ 现有 md/csv ≈ 279 份（含 json/html ≈ 349）。桶划分（沿用 v0.1 口径并更新）：

| 桶 | 说明 | 关键文档 |
|---|---|---|
| index-governance | 入口/基线/追溯/治理 | `00-current-effective-document-entry-v0.1.md`(S0)、`unified-standards-baseline-v0.1.md`、`requirement-traceability-matrix-v0.1.md`、`product-documentation-index-v0.1.md` |
| prd | 产品总纲+模块 PRD | `product-requirements-v0.1.md` + 各域 `*-product-prd-v0.1.md` |
| design-spec | 开发设计/层规格 | `*-development-design.md`、`*-layer-spec-v0.2.md`、`foundation-database-design.md`、`full-admin-srs-v0.1.md` |
| machine-contract | 机器契约 | `openapi/openapi-p0.{json,yaml}` |
| delivery-contract | 验收/operation 矩阵/closure | `p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`*-acceptance-*` |
| security | 安全基线/隔离 | `security-scans/formal-security-baseline-v1.md`、**新增 `data-isolation-and-tenant-scope-security-spec-v0.1.md`** |
| third-party | 供应商接入 | `third-party-supplier-integration-kit-v0.1.md`、`integration-capability-switches-v0.1.md`、**新增 `third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`** |
| ops-readiness | 生产前置 | `server-160-runbook-v0.2.md`、`nas-*`、`production-*`、`production-blocker-register-v0.1.md` |
| plans-reviews / production-evidence / archive | 阶段证据/归档 | `_archive/**`、各 `*-completion-audit-v0.1.md` |

主引用链（仍有效）：S0 入口 → 统一基线/决策日志 → PRD/开发设计 → OpenAPI + operation 矩阵 + closure → 用户故事/任务/权限/事件映射 → 验收与边界用例 → Runbook/blocker/delivery gates。

## 3. 需求-接口-页面-数据表-测试 映射矩阵（矩阵之矩阵）
完整链路由以下既有矩阵分段承接（本审计确认其覆盖关系，并标注数据表链缺口）：

| 链路段 | 承接文档 | 当前态 |
|---|---|---|
| 需求 → 文档 | `requirement-traceability-matrix-v0.1.md`（REQ-01–REQ-92）| 覆盖 |
| 需求 → 代码 | `requirements-code-implementation-matrix-v0.1.csv`（96 项：88 完整/8 部分）| 覆盖 |
| 接口 → 权限/事件/终端/测试 | `p0-openapi-operation-acceptance-matrix-v0.1.md`（207 op）| 覆盖（唯一未漂移的 op 数源）|
| 任务 → 接口 → 权限 → 事件 → 验收 | `p0-task-api-permission-event-acceptance-map-v0.1.md` | 覆盖 |
| 页面 → 权限/字段/UI 验收 | `full-admin-page-permission-field-matrix-v0.1.csv`(30 页) + `full-admin-ui-acceptance-matrix-v0.1.md` | 覆盖 |
| **数据表 → scope/隔离** | **新增 `data-isolation-and-tenant-scope-security-spec-v0.1.md` + `domain-ownership-scope-audit-v0.1.csv`** | 本轮补齐（此前 CSV 为 orphan）|
| 需求 → 数据表 | 散落于 `foundation-database-design.md` + 各域 design | 部分（无单一 REQ→table 矩阵，design 文档按概念组织；可接受）|

## 4. 问题清单（P0/P1/P2/P3，带 file:line）

### P0（会导致开发/验收走错方向）
| # | 问题 | 证据 | 处理 |
|---|---|---|---|
| DOC2-P0-01 | S0 入口 13 处引用**不存在**的 `p0-first-coding-package-v0.1.md`，且列为 S1 必读编码入口/多角色不得跳过 | `00-...entry:23,131,157,196,232,287,290,293,330,361,381,419` | **已修：创建该文件**（按入口定义 + 已实现工件）|
| DOC2-P0-02 | 多租户/商户**数据隔离**无权威 spec（V044–V052 仅 .sql 注释）；安全 finding 台账无跨租户越权条目 | V044–V052；`security-scans/findings-register-v1.md` 无 tenant-isolation | **已修：新增数据隔离规格**（含逐表+注入约定+越权验收）；finding 台账补登记列为 ISO-GAP-01（P0，建议交安全负责人）|
| DOC2-P0-03 | 短信/通知第三方全维度缺失，而登录链路已硬依赖 `sms_code` | `integration-capability-switches-v0.1.md:18-33` 无 sms；OpenAPI 已落 sms_code | **已修：新增 SMS 占位**（开关/字段映射/失败场景/mock）|

### P1（重要，有 workaround）
| # | 问题 | 证据 | 处理 |
|---|---|---|---|
| DOC2-P1-01 | operation 数过期：181/191/198 多处 | `00-...entry:146,322,354`(181 已修)、`development-documentation-adversarial-audit-v0.1.md:58,151`(198)、`gstack-acceptance-ship-audit-v0.1.md:14`、`p0-coding-completion-and-handoff-report-v0.1.md:15,45,46,100,107`、`project-progress-dashboard.md:15` | S0 入口已修为 207；其余历史审计建议加"历史快照、以 0618 audit/op-matrix 为准"注记（见 §6 owner 待办）|
| DOC2-P1-02 | 后端测试数过期：360/528/557/562/670 | `project-progress-dashboard.md:14`、`p0-coding-completion-...:16`、`gstack-...:18`、`p1-coding-completion-audit-v0.1.md:53`、`closed-loop-live-acceptance-audit-20260618.md:11,16`(670) | 我的 0618 audit **已修 670→673**；其余同 P1-01 处理 |
| DOC2-P1-03 | 近期新增文档未挂入任何索引：`closed-loop-live-acceptance-audit-20260618.md`（当前最准实测）、`architecture-simplification-backlog-20260618.md` | 8 个索引文档均 0 命中 | 建议挂入 S0 入口与 audit-map（owner 待办，避免改动入口过多本轮先登记）|
| DOC2-P1-04 | V049 amusement 4 表 merchant_id 无索引；V044–V052 缺 TDD 回归 plan | V049.sql；`superpowers/plans/` 无覆盖 | 登记为隔离规格 ISO-GAP-03/04（工程待办）|
| DOC2-P1-05 | V050 复核持久化表 `supervisor_review_decisions` 字段级无文档 | 仅 V050.sql | **已修：写入数据隔离规格 §7** |
| DOC2-P1-06 | 对象存储/MDM/视频 缺逐项字段映射占位 | `third-party-supplier-integration-kit-v0.1.md:48-76` 缺这三类行 | **已修：新增占位文档 §2** |
| DOC2-P1-07 | `project-progress-dashboard.md` 落后（日期 2026-06-15，停在 P0-only/360 测试/181 op）| `:3,14,15` | 建议重写为当前态（owner 待办，工作量较大）|

### P2（维护性）
| # | 问题 | 处理 |
|---|---|---|
| DOC2-P2-01 | 完成审计/闭环报告高度重复（R1：5 份验收审计；R2：P0/P1/P2 完成审计）| 建议保留 0618 audit 为当前态，其余降级归档（owner）|
| DOC2-P2-02 | `development-documentation-audit-map-v0.1.csv`（297）已滞后且漏收 0618/full-admin-srs/zoom-out 等新文档 | 建议重生成 audit-map（owner）|
| DOC2-P2-03 | dev-design 把 merchant_id 写成 `uuid`，与迁移 `VARCHAR` retrofit 不符 | 登记隔离规格 ISO-GAP-05 |
| DOC2-P2-04 | 约束类安全项（uq_daily_close_scope_date / uq_compliance_reminder_dedup / chk_stock_adjustment_type NOT VALID）仅在 .sql | 已纳入隔离规格 §3；建议进数据完整性说明 |
| DOC2-P2-05 | 占位错误码命名空间缺统一 | **已修：占位文档 §3** |

### P3（噪音）
- `_archive/**` 旧计数（已标历史，不参与开发）。
- 少数表格空行/格式噪音（markdown lint，不影响开发）。

## 5. 本轮已新增/修改的文档（详见汇报）
**新增**：`data-isolation-and-tenant-scope-security-spec-v0.1.md`、`p0-first-coding-package-v0.1.md`、`third-party-sms-notification-and-supplier-field-mapping-placeholders-v0.1.md`、本审计 `development-documentation-audit-v0.2.md`。
**修改**：`00-current-effective-document-entry-v0.1.md`（181→207 三处 + 注记）、`closed-loop-live-acceptance-audit-20260618.md`（670→673）。

## 6. 移交 owner 的待办（本轮未自动改，避免大范围改动历史/入口）
1. 历史阶段审计（gstack/handoff/p1-audit/dashboard）加"历史快照"注记或重写当前态；统一改为引用 op-matrix 与 `verify:*` 实时输出，根治计数漂移。
2. 把 0618 两份文档 + 数据隔离规格 + 第三方占位 + 本 v0.2 挂入 S0 入口与重生成的 audit-map。
3. 安全：在 `findings-register-v1.md` 补 `SEC-*-TENANT-ISOLATION-*` 系列 + 新增 `verify:tenant-isolation` 闸口（ISO-GAP-01/02）。
4. 工程：V049 补索引、V044–V052 补 TDD 回归 plan、旧行 scope 回填（隔离规格 §5/§6）。

## 7. 是否可进入各阶段（结论不变）
开发/联调/产品测试验收：可以。完整后台/真实供应商联调：条件可以（按 SRS/供应商占位）。公开生产上线：不可以（外部证据未关闭）。
