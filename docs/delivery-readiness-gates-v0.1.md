# 交付总闸口与上线准入验证 V0.1

## 1. 定位

本文档把当前已经形成的分散验证命令收口为一个交付总闸口，用于在进入交付、外部评审、生产前置实施或供应商联调前做统一检查。

它不是新业务范围，也不是完整回归测试替代品；它只回答一个问题：当前仓库是否满足“只能在 160 正式编码、外部依赖不得伪实现、P0 契约仍闭合、安全基线仍可生成”的最低交付准入。

机器识别标记：

- DELIVERY_READINESS_GATES_READY
- VERIFY_DELIVERY_GATES_READY
- NO_DELIVERY_WITHOUT_EXTERNAL_DEPENDENCY_GATES
- NO_FORMAL_CODE_OUTSIDE_160
- NO_PRODUCTION_DEPLOYMENT_WITH_PENDING_EXTERNAL_BLOCKERS
- OWNER_ACTION_PACKS_FROM_PRODUCTION_BLOCKERS
- PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY

## 2. 必跑命令

交付前先运行：

```bash
npm run verify:delivery-gates
```

该命令应保持轻量，不直接包含后端完整测试、Web 构建、Android 构建或生产发布动作。它串联以下闸口：

| 顺序 | 命令 | 目的 |
|---|---|---|
| 1 | `npm run verify:delivery-gates-docs` | 校验本文档、入口文档、交付包清单和脚本是否互相引用 |
| 2 | `npm run verify:coding-host-protection` | 确认正式代码只在 192.168.88.160 的标准仓库执行 |
| 3 | `npm run verify:external-dependency-gates` | 阻断未确定供应商前的真实机器人、视频、设备控制和独立 Python AI 服务伪实现 |
| 4 | `npm run verify:t3-coding-package` | 校验 T3 编码包、供应商闸口和只读/后置边界仍闭合 |
| 5 | `npm run verify:t3b-supplier-mock-kit` | 校验供应商 mock kit 仍不包含真实密钥、生产控制或越权闭环 |
| 6 | `npm run verify:t3d-local-llm-ai-quality` | 校验 160 本地大模型质检仍为 loopback-only、脱敏、人工复核建议 |
| 7 | `npm run verify:p0-contract` | 校验 P0 OpenAPI、示例、operation 矩阵、闭环附录和客户端契约 |
| 8 | `npm run verify:security-baseline` | 校验正式安全基线输入和生产文档访问策略 |
| 9 | `npm run verify:production-readiness-evidence` | 校验生产前置证据、台账状态和安全基线 evidence 是否一致 |
| 10 | `npm run verify:production-blockers` | 校验生产阻断清单与台账、findings register 同步 |
| 11 | `npm run verify:production-owner-actions` | 校验生产阻断项已按责任人拆成可执行动作包，且动作包由阻断清单自动生成 |
| 12 | `npm run verify:production-evidence-submission-kit` | 校验证据提交包由责任人动作包自动生成，且仍保持等待真实证据状态 |
| 13 | `npm run verify:production-evidence-intake` | 校验生产前置证据回填模板齐全且不包含明文密钥 |

## 3. 交付阻断规则

1. 未通过 `verify:coding-host-protection`：不允许宣称当前代码来自正式编码仓库。
2. 未通过 `verify:external-dependency-gates`：不允许进入供应商联调、生产前置签收或对外交付。
3. 生产前置台账仍存在 `pending_info` 的外部能力，例如 MDM、支付、企微、人脸、供应商 API、第三方密钥时，不允许宣称生产闭环完成。
4. 未拿到真实供应商 SDK/API、签名、回调、字段、SLA 和测试账号前，只允许保留 OpenAPI 边界、mock adapter、台账和 Runbook，不允许做真实控制闭环。
5. 独立 Python AI 服务在 T3-D2 前继续阻断；当前允许的是 160 本地大模型只读建议能力，不允许 AI 自动处罚、自动退款、自动调价、自动核销或自动影响绩效。

## 4. 与完整验证的关系

`npm run verify:delivery-gates` 是交付准入，不是完整上线验收。正式上线仍需按场景补跑：

| 场景 | 追加验证 |
|---|---|
| 后端代码变更 | `npm run test:backend` |
| Web 管理端变更 | `npm run build` 和对应功能校验 |
| Android/一体机变更 | `npm run verify:android-env`、`npm run android:p0:build`、`npm run android:p0:apk` |
| OpenAPI 变更 | 先跑 `npm run docs:openapi-closure`，再跑 `npm run verify:p0-contract` |
| 生产运维前置 | `npm run verify:ops160-recovery`，并回填真实证据 |
| 安全发布前 | Semgrep、差异审计、密钥扫描、人工 code review |

## 5. 失败处理

失败时不要跳过闸口，也不要改脚本掩盖问题。处理顺序如下：

1. 先看失败命令对应的文档或工具提示。
2. 如果是 OpenAPI 附录漂移，先生成闭环附录，再复跑契约闸口。
3. 如果是外部依赖阻断，回到供应商资料、生产前置台账和外部依赖闸口，不写假实现。
4. 如果是 160 主机保护失败，先确认是否在正式仓库 `/home/james/complex-management` 和主机 `192.168.88.160` 上运行。
5. 如果是责任人动作包或证据提交包漂移，先运行 `npm run docs:production-blockers`，再运行 `npm run docs:production-owner-actions` 和 `npm run docs:production-evidence-submission-kit`。
6. 修复后必须重新运行 `npm run verify:delivery-gates` 并保留输出证据。