# P1 编码完成审计 V0.1

> ⚠️ 历史快照。本文 operation/测试计数（如 528/557 后端测试）为历史快照，已过期。当前以 `p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准（当前 209 ops / 729 测试 / Flyway V060）；另见 `closed-loop-live-acceptance-audit-20260618.md`。下文历史结论保留作追溯。

## 1. 结论

截至 2026-06-15，当前可在 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 工程内直接落地的 P1 后端能力已完成编码、接口契约、数据库迁移、权限、审计事件和测试验证。

P1 不再新增业务范围；供应商 SDK/API、硬件能力、真实渠道字段、视频/机器人/设备回调等依赖外部资料的内容，按适配器和占位契约保留，不作为本轮可编码缺口。

本轮补充完成风险预警处理权限加固：`resolveRiskAlert` 属于写操作，已从 `analytics.risk_alert.view` 收紧为 `analytics.risk_alert.manage`，并同步控制器、服务审计元数据、OpenAPI、operation 矩阵和闭环附录。

## 2. 已完成 P1 能力

| 领域 | 能力 | 交付口径 |
| --- | --- | --- |
| 酒吧 | 卡座转卡、卡座合并、续卡 | 已有 table session 调整接口、权限、留痕和测试 |
| 酒吧 | 服务费、低消差额补齐、挂账 | 已有账单调整、AR/AP 台账关联、权限和测试 |
| 酒吧 | 生产票流转 | 已有制作票、接单/制作/完成/取消事件和测试 |
| 酒吧 | 账单拆单、合单 | 已有 V033、操作记录、幂等、权限和测试 |
| 酒吧 | 员工提成 | 已有销售/非销售 15%/10% 提成计算和测试 |
| 酒吧 | 商品成本字段 | 已在商品主数据保留成本字段；BOM/配方级重算待库存成本主数据成熟 |
| 票务 | 退款行台账 | 已有退款行、金额校验、幂等、权限和测试 |
| 票务 | 会员价变体 | 已在票价规则支持 `member`，基于平日/周末/节假日基础价派生 |
| 客人端/会员 | 积分明细、兑换目录、积分兑换 | 已有客人 token 边界、兑换库存扣减、幂等、事件和测试 |
| 游乐 | 安全告知、电子免责、事故工单 | 已有接口、权限、留痕和测试 |
| 游乐 | 预约/排队轻闭环 | 已有预约、排队、状态变更、项目/场次校验和测试 |
| 商户 | 服务/维修/投诉/整改工单 | 已有工单接口、权限、状态流转和测试 |
| 商户 | 商户账单门户 | 已有账单读取、应收口径、权限和测试 |
| 商场平面图 | 图片 + 多边形坐标 | 已有楼层图、区域多边形、版本和测试 |
| 库存 | 预警与对账 | 已有预警、差异记录、权限和测试 |
| 合同 | 履约和到期提醒 | 已有合同合规/到期接口、权限和测试 |
| 风控分析 | 风险预警处理权限加固 | `resolveRiskAlert` 已要求 `analytics.risk_alert.manage`，查看类接口仍保持 `analytics.risk_alert.view` |
| 第三方 | 重试、补偿、对账 | 已有集成任务、回调/轮询校验、差异补偿和测试 |

## 3. 本轮新增/收口文档

| 文档 | 用途 |
| --- | --- |
| `p1-amusement-reservation-queue-light-api-contract-v0.1.md` | 游乐预约/排队轻闭环接口契约 |
| `p1-ticket-member-price-variant-api-contract-v0.1.md` | 会员价变体接口契约 |
| `p1-bar-bill-split-merge-api-contract-v0.1.md` | 酒吧账单拆单/合单接口契约 |
| `p1-coding-completion-audit-v0.1.md` | P1 编码收口、验收证据和剩余依赖说明 |
| `docs/superpowers/plans/2026-06-15-p1-risk-alert-permission-hardening.md` | 风险预警处理权限加固的 TDD 执行计划和验证证据 |

## 4. 验证证据

在 192.168.88.160 的 `/home/james/complex-management` 执行：

| 验证项 | 结果 |
| --- | --- |
| `cd backend && mvn -Dtest=AnalyticsControllerTests#analyticsRoutesUseExactOpenApiPermission test` | 先失败后修复，证明测试能捕捉 `resolveRiskAlert` 误用 `view` 权限的问题 |
| `cd backend && mvn -Dtest=AnalyticsControllerTests,AnalyticsServiceTests test` | 通过，26 个测试，0 失败，0 错误 |
| `cd backend && mvn -Dtest=AnalyticsControllerTests,AnalyticsServiceTests,AnalyticsJdbcIntegrationTests test` | 通过，30 个测试，0 失败，0 错误 |
| `cd backend && mvn test` | 通过，528 个测试，0 失败，0 错误 |
| `npm run verify:p0-contract` | 通过，OpenAPI、示例、operation 矩阵、闭环附录、客户端路径和 payload 策略均通过 |
| `semgrep --metrics=off --config p/java --error backend/src/main/java backend/src/main/resources/db/migration` | 通过，374 个 Java 目标，60 条规则，0 findings |
| `semgrep --metrics=off --config p/java --error backend/src/main/java/com/complex/platform/analytics backend/src/test/java/com/complex/platform/analytics` | 通过，本轮增量权限加固相关 7 个目标，60 条规则，0 findings |

## 5. 不作为当前 P1 可编码缺口的事项

| 事项 | 原因 | 后续进入条件 |
| --- | --- | --- |
| 机器人、视频平台、设备厂商真实 SDK/API 字段 | 依赖供应商最终资料和签名/回调规范 | 供应商 API/SDK、字段字典、签名样例和联调环境到位 |
| 酒吧年龄/人脸核验生产级供应商能力 | 依赖采购和第三方接口 | 供应商选型、授权、SDK/API 和合规材料到位 |
| 视频证据平台内播放 | 依赖视频平台播放鉴权和链接时效规则 | 视频平台 API、播放 token、审计回调规则到位 |
| BOM/配方级成本重算 | 依赖库存成本、配方、批次和损耗主数据 | 库存/BOM 成本主数据建模完成后再做成本重算 |
| 团体/包场、集团视图、动态容量策略 | 已按路线图属于 P2 或更高复杂度 | 总控重新排期并给出容量策略和运营规则 |

## 6. 审计结论

P1 当前编码口径已收口。后续若继续开发，应进入 P2 或供应商联调专项，不应再把外部依赖项反向认定为 P1 后端未完成。

## 7. 2026-06-15 UI 自动化与历史审计摘要更新

本轮补齐 Web 管理端 UI 自动化验收，并将历史审计中的 P1-4 摘要同步为当前口径：P1 后端能力已收口；Web 管理端作为轻量管理/只读与有限操作入口，不按完整后台替代品验收；动态定价属于 P2 能力；当前已升级为草稿模拟、发布后正式订单生效，不反向认定为 P1 未完成。

新增验收证据：

| 验证项 | 结果 |
| --- | --- |
| `npm run verify:ui` | 通过，7 个 Playwright UI 验收，覆盖登录/退出、票务出票、动态定价草稿/发布/模拟、供应商适配器创建、团体/包场释放、权限错误、空状态和窄屏 |
| `npm run build` | 通过，Vite 拆分 React、AntD、icons 和 vendor chunk，主包过大 warning 已消除 |
| `npm run verify:p0` | 通过，含契约、安全基线、后端 557 个测试、Web 构建、Android 环境/构建/冒烟/APK |

详细矩阵见 `docs/ui-automation-acceptance-report-v0.1.md`。
