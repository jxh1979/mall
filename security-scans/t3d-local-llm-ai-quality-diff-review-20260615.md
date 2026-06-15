# T3-D1 本地大模型 AI 质检建议差异安全审查 V0.1

审查日期：2026-06-15
审查范围：T3-D1 local LLM AI quality suggestion working-tree diff。

## 1. 审查结论

未发现阻断性安全问题。

本轮变更新增的是 160 机器本地大模型辅助质检建议切片，不是独立 Python AI 服务，也不是自动处罚、退款、核销、调价、绩效或合同状态决策链路。

## 2. 高风险面检查

| 风险面 | 结论 |
|---|---|
| 鉴权与权限 | 新接口要求 Bearer 认证，并使用 `analytics.risk_alert.manage` 权限。 |
| 幂等 | 新写接口要求 `idempotency_key`，并纳入统一幂等执行链。 |
| 外部调用 | `OpenAiCompatibleLocalLlmClient` 默认禁用，仅允许 `http/https + 127.0.0.1/localhost/::1`。 |
| 密钥 | T3-D1 不写入 Authorization/Bearer/API key；Semgrep secrets 扫描 0 findings。 |
| 敏感数据 | 调用模型前脱敏手机号、身份证、邮箱和银行卡号，并限制输入长度。 |
| AI 输出可信度 | 模型输出被视为不可信数据；质量分、置信度、风险等级、摘要和 finding 数量均归一化，异常输出强制人工复核。 |
| 数据库迁移 | V040 仅扩大 AI 质检任务 `processing_mode` 约束，允许 `local_llm_suggestion`。 |
| 审计留痕 | 成功/失败事件进入 OpenAPI、operation 矩阵和闭环附录。 |

## 3. 本轮修复项

- 补强模型输出边界：非法 `risk_level` 不再导致用户请求被错误判为 400，而是归一化为 `suspicious` 并强制人工复核。
- 补强本地模型 URL 边界：非 HTTP/HTTPS 的 loopback URL 也被拒绝，避免协议混用。
- 补齐适配器单元测试：默认禁用、非 loopback、非 HTTP/HTTPS loopback 均覆盖。

## 4. 已运行验证

- `npm run verify:t3d-local-llm-ai-quality`：通过。
- `npm run verify:p0-contract`：通过，191 个 operation 同步。
- `npm run test:backend`：通过，555 个测试，0 失败。
- `npm run build`：通过，仅保留既有 Vite chunk size 警告。
- `npm run verify:security-baseline`：通过。
- `semgrep --no-git-ignore p/owasp-top-ten + p/secrets`：扫描 1293 个文件，0 findings。
- `git diff --check`：通过。

## 5. 剩余风险

- 真实本地模型服务仍需运维显式启动并绑定 loopback 地址；不得暴露到局域网或公网。
- 模型质量、提示词和评估样本仍属于后续 AI 治理，不影响当前 T3-D1 作为“辅助建议”的可编码结论。
- 若后续升级为独立 Python AI 服务或真实外部模型服务，必须重新走 T3-D2 设计、安全基线和供应商/模型治理审查。
