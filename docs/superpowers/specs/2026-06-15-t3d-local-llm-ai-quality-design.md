# T3-D1 160 本地大模型 AI 质检建议设计

## 1. 结论

T3-D1 可以启动，但只能作为 160 本地大模型只读建议切片。它不替代 Java 规则处理器，不直接改变付款、核销、退款、处罚、绩效、合同、库存或设备控制状态。

机器标记：T3D_LOCAL_LLM_AI_QUALITY_DESIGN_READY

## 2. 当前上下文

- 160 机器具备 RTX 4090 Laptop GPU 和本地 GGUF/safetensors 模型文件。
- 当前未发现稳定运行中的 Ollama、llama.cpp 或 LM Studio OpenAI 兼容 API 服务。
- 后端已存在 analytics_ai_quality_tasks 和 analytics_ai_quality_results，适合作为 T3-D1 的落点。
- P2-F 原口径是独立 Python AI 服务暂缓；本次只把 T3-D1 调整为 Java 后端调用本机 127.0.0.1 模型服务的受控适配层。

## 3. 目标

1. 在 Java 后端增加本地 LLM 适配器边界。
2. 提供一个受权限、幂等和审计保护的 AI 质检建议生成入口。
3. 输入只允许最小必要文本，并在调用模型前进行脱敏和长度限制。
4. 模型不可用、超时、返回不可解析或低置信度时，结果必须进入人工复核口径。
5. 所有输出只写入 AI 质检任务和结果，不写核心业务主表。

## 4. 不做什么

- 不启动完整独立 Python AI 服务。
- 不新增真实模型密钥。
- 不把文本发送到非本机地址。
- 不处理身份证、人脸原图、支付凭证、银行卡、明文手机号等敏感原文。
- 不让 AI 输出直接影响交易、通行、核销、处罚、提成、退款、合同和设备控制。

## 5. 推荐架构

流程：Web 或运营后台 -> AnalyticsController -> AnalyticsService -> AiTextRedactor -> LocalLlmClient -> 127.0.0.1 OpenAI compatible API。结果只写 analytics_ai_quality_tasks、analytics_ai_quality_results 和审计事件。

## 6. 接口设计

新增后端接口：POST /api/v1/analytics/ai-quality/local-llm-suggestions

权限：analytics.risk_alert.manage

幂等：必须传 idempotency_key

审计成功事件：analytics.ai_quality.local_llm_suggestion.created

审计失败事件：analytics.ai_quality.local_llm_suggestion.failed

请求字段：

| 字段 | 规则 |
|---|---|
| idempotency_key | 必填，最多 128 字符 |
| source_type | 必填，最多 64 字符，例如 ticket_call、wecom_message |
| source_id | 必填，最多 64 字符 |
| source_text | 必填，最多 12000 字符；调用模型前截断到配置上限 |
| requested_by | 可选，最多 64 字符 |

响应字段：

| 字段 | 含义 |
|---|---|
| task | AI 质检任务记录 |
| result | AI 质检建议结果 |
| model_status | completed、unavailable、failed |
| redaction_applied | 是否执行脱敏 |
| manual_review_required | 是否必须人工复核 |

## 7. 本地模型适配

默认配置：

- complex.analytics.local-llm.enabled=false
- complex.analytics.local-llm.base-url=http://127.0.0.1:1234/v1
- complex.analytics.local-llm.model=local-model
- complex.analytics.local-llm.timeout=PT8S
- complex.analytics.local-llm.max-input-chars=4000

安全要求：

- base URL 只允许 127.0.0.1、localhost、::1。
- 调用 /chat/completions，兼容 LM Studio、llama.cpp server 或 Ollama OpenAI 兼容层。
- 禁止把真实密钥、生产账号或敏感原文写入日志、审计事件和文档。

## 8. 失败与降级

| 场景 | 行为 |
|---|---|
| 本地模型未启用 | 创建任务和人工复核结果，manual_review_required=true |
| 模型超时 | 创建任务和人工复核结果，审计失败原因只写摘要 |
| 模型返回非 JSON | 创建任务和人工复核结果 |
| 模型置信度低于 0.60 | 保留结果，但标记人工复核 |
| 输入包含手机号、身份证、银行卡或邮箱 | 调用前脱敏，响应标记 redaction_applied=true |

## 9. 验收标准

1. 本地 LLM 默认关闭时，接口仍返回可审计的人工复核建议，不抛 500。
2. fake LLM 返回正常结果时，服务保存 AI 质检任务和结果。
3. 输入文本中的手机号、身份证、银行卡和邮箱不会进入 LLM client。
4. 非本机 base URL 被拒绝或降级，不允许外传。
5. 后端测试、OpenAPI 契约、安全基线和 Semgrep 均通过。
