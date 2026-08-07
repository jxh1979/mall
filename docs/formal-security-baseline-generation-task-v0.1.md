# 正式安全基线生成与维护任务 V0.1

> **口径修订（2026-08-06 R10）：「人工放行」已废止**——ENUM「核销记录状态」条 明确「**系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（DEC-CFG-2026-08-03-12 / DEC-R6-08）。本文原有的「人工放行」取值、端点、按钮、指标、权限点与复核类型**一律废止**，废止处就地标注「原含『人工放行』，已废止·见文首口径」；已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入。**注意**：合同或基础定义违规被卡住后「由更高级审核放行」的准入场景仍然存在，那是准入阻断的升级放行，与核销域无关，不得回读为「可以人工放行一笔核销」。

## 1. 文档定位

本文档定义“正式安全基线”何时生成、由谁生成、需要哪些输入、产出哪些报告和如何验收。它不是最终安全报告，也不继承旧 TypeScript/Fastify/SQLite 原型安全扫描结论。

当前正式安全基线只针对 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven + springdoc-openapi + Android/一体机/开放平台/160 运维链路。旧安全扫描仅保留历史追溯价值，不得作为当前上线依据。

## 2. 触发条件

正式安全基线不得在只有文档和 smoke 切片时草率生成。满足下列条件后必须生成 V1 安全基线：

| 条件 | 最低要求 |
|---|---|
| BE-000 通过 | 正式后端工程可启动，Spring Boot 4.x、PostgreSQL、Flyway、springdoc 可运行 |
| 首批编码包完成 | `p0-first-coding-package-v0.1.md` 的 CP-BE/CP-DB/CP-AUTH/CP-EVT/CP-OPS 至少有可运行实现 |
| 运行时 OpenAPI 可导出 | `/docs/openapi` 或等效地址可导出运行时契约，并能与 `docs/openapi/openapi-p0.yaml/json` 比对 |
| 160 或等效预生产可验 | `complex-runtime-config-check.sh`、`complex-verify-be000.sh`、`complex-health-check.sh` 可执行 |
| 生产前置台账存在 | MDM、NAS、密钥轮换、支付、人脸年龄核验、企业微信、第三方供应商等前置项已有状态记录；`npm run verify:ops160-recovery` 可验证 NAS 恢复脚本和生产凭证轮换台账结构 |

## 3. 安全基线输出物

| 输出物 | 内容 | 阻断规则 |
|---|---|---|
| threat-model-v1 | 资产、角色、信任边界、攻击面、数据流、主要威胁和缓解措施 | 缺开放平台、支付、设备、证据、个人信息、160 运维边界时不通过 |
| authz-review-v1 | 登录、会话、权限模板、临时授权、数据范围、显式拒绝、高风险审批 | 任一高风险 operation 无权限或无数据范围时不通过 |
| openapi-exposure-review-v1 | OpenAPI 暴露面、认证上下文、签名、幂等、错误码和敏感字段 | 写接口无幂等、外部接口无签名/防重放时不通过 |
| webhook-signature-review-v1 | Webhook、轮询校验、回调重放、补偿、对账、失败重试 | 回调可重复扣减、漏回调无补偿时不通过 |
| payment-risk-review-v1 | 聚合支付、直连接口抽象、退款、对账、异常冲正 | 退款和对账无审计、无异常补偿时不通过 |
| pii-retention-review-v1 | 个人信息、实名范围、同意、撤回、保留周期、脱敏和访问审计 | 超出法定必要范围强制实名或无同意留痕时不通过 |
| device-mdm-review-v1 | Android Enterprise/MDM、设备绑定、Root/越狱、终端能力开关、离线边界 | 未纳管设备可生产核销高风险权益时不通过 |
| ops-160-review-v1 | 160 配置、systemd、Nginx、日志、备份、恢复、账号密钥轮换 | 密钥明文落文档、无恢复演练、无轮换台账、`verify:ops160-recovery` 不通过时不通过 |
| dependency-sbom-review-v1 | Maven、npm、Android 依赖清单、许可证和高危漏洞处置 | 存在未处置高危漏洞且无豁免记录时不通过 |
| findings-register-v1 | P0/P1/P2/P3 风险、责任人、截止时间、状态、豁免记录 | P0 风险未修复或未正式豁免时不允许上线 |

## 4. 输入资料

| 输入 | 用途 |
|---|---|
| `security-scans/threat_model.md` | 正式安全基线重建入口 |
| `docs/openapi/openapi-p0.yaml` / `docs/openapi/openapi-p0.json` | 静态契约基线 |
| 运行时 `/docs/openapi` | 实际暴露面比对 |
| `p0-openapi-contract-closure-appendix-v0.1.md` | 权限、事件、任务、终端和样例追溯 |
| `p0-operation-permission-matrix-v0.1.md` | 权限模板、临时授权、显式拒绝和审批 |
| `p0-action-event-evidence-matrix-v0.1.md` | 事件、证据、复核和敏感访问留痕 |
| `third-party-supplier-integration-kit-v0.1.md` | 双向开放、签名、Webhook、轮询校验和补偿 |
| `mdm-android-enterprise-implementation-plan-v0.1.md` | 设备管控和终端生产前置 |
| `production-credential-rotation-runbook-v0.1.md` | 账号、密钥、数据库、对象存储、JWT/Token、聚合支付、企业微信、第三方和开放平台凭证轮换 |
| `nas-backup-and-restore-runbook-v0.1.md` | NAS 备份恢复和演练 |
| `production-readiness-implementation-ledger-template-v0.1.md` | 生产前置实施状态和证据链接 |

## 5. 生成流程

| 步骤 | 动作 | 输出 |
|---|---|---|
| 1 | 固定版本快照 | 记录代码版本、OpenAPI hash、数据库迁移版本、160 release |
| 2 | 对比契约 | 对比静态 OpenAPI 和运行时 OpenAPI，列出差异 |
| 3 | 识别资产和边界 | 标出客户、员工、商户、票券、支付、证据、设备、第三方、对象存储和 NAS |
| 4 | 枚举攻击面 | 后台 Web、App/小程序、Android 工作手机、平板、一体机、开放平台、Webhook、160 运维 |
| 5 | 审核权限和数据范围 | 逐 operation 检查认证上下文、权限码、数据范围和审计事件 |
| 6 | 审核高风险业务 | 支付退款、核销、卡座、酒吧年龄核验、高价值酒水、应收应付调整（原含「人工放行」，已废止·见文首口径） |
| 7 | 审核第三方边界 | 签名、防重放、Webhook、轮询校验、字段映射、异常补偿和供应商失败模式 |
| 8 | 审核运维安全 | 160 配置、systemd、日志、备份恢复、密钥轮换、最小权限；执行 `npm run verify:ops160-recovery` |
| 9 | 出具发现清单 | 按 P0/P1/P2/P3 分级，明确责任人和截止时间 |
| 10 | 复验与归档 | P0 修复后复验；P1/P2/P3 进入后续排期或风险接受 |

## 6. 验收标准

| 场景 | Given | When | Then |
|---|---|---|---|
| 运行时契约审计 | 静态 OpenAPI 与运行时 OpenAPI 都可获取 | 执行契约差异检查 | 不得出现未登记生产路径、未授权写接口或缺失审计事件的差异 |
| 权限审计 | 181 个 P0 operation 已进入追溯附录 | 审核认证上下文和权限码 | 高风险 operation 必须有权限、数据范围、幂等和审计事件 |
| Webhook 审计 | 供应商回调、支付回调和开放平台回调存在 | 审核签名、nonce、时间戳和幂等 | 重放、重复回调、漏回调均有拒绝或补偿策略 |
| 个人信息审计 | 存在客户、员工、会员、实名或人脸年龄核验数据 | 审核采集、同意、脱敏、访问和留存 | 只在法定必要范围实名；敏感访问有审计和复核 |
| 运维审计 | 160 或等效预生产已部署 | 审核配置、密钥、日志、备份、恢复 | 不打印密钥；备份可恢复；轮换有台账和到期提醒 |
| 文档暴露审计 | Springdoc/OpenAPI/Swagger 已接入 | 执行 `npm run verify:prod-docs-access` | prod profile 下 `/docs`、`/docs/**`、`/swagger-ui.html`、`/swagger-ui/**`、`/v3/api-docs`、`/v3/api-docs/**` 不得匿名公开；dev/test 可保留契约导出能力 |
| 缺陷收口 | 发现 P0/P1/P2/P3 风险 | 复验或风险接受 | P0 未修复不得上线；P1/P2/P3 必须有责任人和计划 |

## 7. 强制反模式

| 反模式 | 结论 |
|---|---|
| 复用旧 TypeScript/SQLite 原型威胁模型作为当前报告 | 阻断 |
| 只扫描依赖漏洞，不做业务权限和数据流威胁建模 | 阻断 |
| 只看静态 OpenAPI，不比对运行时暴露路径 | 阻断 |
| 高风险 operation 无权限、无幂等、无审计仍标记通过 | 阻断 |
| 真实密钥、数据库密码、对象存储密码写入报告正文 | 阻断 |
| 备份未恢复演练却标记可恢复 | 阻断 |

## 8. 当前结论

截至 2026-06-15，正式安全基线 V1 已生成并纳入机器校验：`security-scans/formal-security-baseline-v1.md`、`security-scans/findings-register-v1.md`、`docs/production-readiness-evidence/security-baseline-latest.json`。旧 TypeScript/Fastify/SQLite 安全基线继续失效；后续只要 OpenAPI、认证、权限、第三方接入、支付、MDM、生产凭证、160 运维或供应商证据发生实质变化，必须重新执行 `npm run docs:security-baseline && npm run verify:security-baseline && npm run verify:production-readiness-evidence`。
