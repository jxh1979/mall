# T3-D1 Local LLM AI Quality Slice V0.1

Machine marker: T3D_LOCAL_LLM_AI_QUALITY_READY
Machine marker: LOOPBACK_ONLY_LLM
Machine marker: AI_OUTPUT_NO_AUTO_DECISION

## 1. Scope

T3-D1 enables a safe local model suggestion path on host 160. The backend calls only a loopback OpenAI-compatible local model endpoint and stores the output as AI quality task/result records.

This slice does not start the future independent Python AI service. It does not perform automatic decisions.

## 2. Runtime boundary

Default configuration:

- complex.analytics.local-llm.enabled=false
- complex.analytics.local-llm.base-url=http://127.0.0.1:1234/v1
- complex.analytics.local-llm.model=local-model
- complex.analytics.local-llm.timeout=PT8S

Allowed model serving options after manual ops setup:

- LM Studio OpenAI-compatible local server
- llama.cpp server with OpenAI-compatible endpoint
- Ollama OpenAI-compatible endpoint if bound to loopback only

The adapter rejects or degrades non-loopback base URLs. Do not expose this model endpoint to LAN or internet in T3-D1.

## 3. API

Endpoint:

POST /api/v1/analytics/ai-quality/local-llm-suggestions

Governance:

- Permission: analytics.risk_alert.manage
- Auth context: staff-token
- Idempotency: required
- Success event: analytics.ai_quality.local_llm_suggestion.created
- Failure event: analytics.ai_quality.local_llm_suggestion.failed

## 4. Data handling

Before invoking the local model, backend redacts:

- phone number -> [PHONE_REDACTED]
- ID card-like value -> [ID_REDACTED]
- email -> [EMAIL_REDACTED]
- card-like number -> [CARD_REDACTED]

The model input is truncated to 4000 characters. Request source_text is capped at 12000 characters.

## 5. No automatic decision rule

AI output is only an operational suggestion. It must not directly change payment, refund, ticket verification, punishment, commission, contract, inventory, or device state.

If the model is disabled, unavailable, times out, returns invalid JSON, or has confidence below 0.60, the response must require manual review.

## 6. Verification

Run:

- npm run verify:t3d-local-llm-ai-quality
- npm run test:backend -- -Dtest=AnalyticsServiceTests,AnalyticsControllerTests,AnalyticsJdbcIntegrationTests
- npm run verify:p0-contract
- npm run verify:security-baseline

## 7. Production note

T3-D1 is not a production AI decision engine. Production use still requires model runtime runbook, capacity test, prompt/version governance, manual review SOP, monitoring, and data compliance review.
