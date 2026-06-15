# 外部依赖准入门 V0.1

机器标记：EXTERNAL_DEPENDENCY_GATES_READY
机器标记：NO_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY
机器标记：NO_INDEPENDENT_PYTHON_AI_SERVICE_WITHOUT_GATE
机器标记：PRODUCTION_EXTERNAL_BLOCKERS_TRACKED
验证命令：`npm run verify:external-dependency-gates`

## 1. 结论

当前可编码范围已经覆盖 P0/P1/P2 可落地切片，以及 T3-A 只读分析、T3-B mock 套件、T3-D1 160 本地大模型只读质检建议。剩余外部依赖项不能继续写生产实现，只能保持准入门和台账化跟踪。

本准入门的目标不是阻止后续联调，而是防止在资料未到位时把 mock、占位、示例或本地模型能力误标成真实生产完成。

## 2. 仍阻断真实生产实现的事项

| 事项 | 当前状态 | 允许继续做 | 禁止继续做 | 解锁资料 |
| --- | --- | --- | --- | --- |
| 机器人平台真实控制 | blocked | 字段映射、mock、签名样例、只读分析 | 下发真实任务、调度、停止、移动或远程控制指令 | API/SDK、签名、回调、错误码、沙箱、额度、验收证据 |
| 视频平台真实调阅/证据绑定 | blocked | mock 链接、证据字段、权限设计 | 真实播放、片段截取、自动证据绑定 | 播放鉴权、录像索引、保留期限、调阅审计协议 |
| 设备厂商/NFC/闸机真实控制 | blocked | 设备能力字典、mock 状态、开放接口占位 | 开闸、锁定、放行、远程控制、离线高风险核销 | 硬件型号、SDK、签名、网络条件、失败回退 |
| 酒吧年龄/人脸生产核验 | pending_info | mock、接口占位、失败阻断逻辑 | 生产环境仅靠 mock 或人工口头确认标记通过 | 供应商 SDK/API、合规材料、测试账号、错误码 |
| 聚合支付生产接入 | pending_info | 抽象接口、mock 支付、对账口径 | 真实收款、退款、生产对账 | 商户号、密钥引用、回调验签、退款权限、对账文件 |
| 企业微信生产同步/深度质检 | pending_info | 基础同步占位、规则质检、失败告警设计 | AI 自动处罚或自动绩效处理 | 应用配置、通讯录/会话存档权限、回调、同步证据 |
| 独立 Python AI 服务 T3-D2 | blocked | Java 内置规则、本地 loopback LLM 只读建议 | 替换生产路径、独立 AI 服务直接影响业务决策 | 模型方案、样本集、SLA、部署监控、安全审查、数据合规 |

## 3. 当前允许的替代范围

- T3-A：只读经营分析，不发控制指令。
- T3-B：供应商 SDK/mock 套件，不包含真实密钥和生产控制。
- T3-D1：160 本地大模型只读质检建议，只走 loopback-only，不发送未脱敏敏感信息，不自动影响业务状态。
- 生产前置：通过台账记录 `pending_info`、责任人、证据链接、下一步动作和阻塞范围。

## 4. 验收标准

- `npm run verify:external-dependency-gates` 必须输出 `EXTERNAL_DEPENDENCY_GATES_PASS`。
- `npm run verify:t3-coding-package` 必须继续输出 `T3_CODING_PACKAGE_PASS`。
- `npm run verify:t3b-supplier-mock-kit` 必须继续输出 `T3B_SUPPLIER_MOCK_KIT_PASS`。
- `npm run verify:t3d-local-llm-ai-quality` 必须继续输出 `T3D_LOCAL_LLM_AI_QUALITY_PASS`。
- OpenAPI 不得出现真实机器人/视频/设备控制路径或独立 Python AI 服务路径。
- 仓库不得出现独立 Python AI 服务目录、systemd 服务或 docker compose 服务。
- 生产前置台账必须保留 MDM、支付、人脸、企业微信、第三方供应商和三方密钥的 `pending_info` 或等效未通过状态，直到真实资料和证据回填。

## 5. 回滚和解锁

- 若后续供应商资料到位，先新增供应商专项计划和字段映射，再改本准入门状态。
- 解锁真实控制前，必须新增 OpenAPI operation、权限、审计事件、回调验签、失败回退、测试和 Semgrep 证据。
- 解锁独立 Python AI 服务前，必须新增部署拓扑、SLA、降级 Runbook、模型评估和数据合规审计。

## 6. 变更记录

| 日期 | 版本 | 说明 |
| --- | --- | --- |
| 2026-06-15 | V0.1 | 新增外部依赖总准入门，把 T3-C、T3-D2、支付、企业微信、人脸和真实设备控制统一纳入机器可验证阻断。 |