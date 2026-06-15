# T3-B 供应商 SDK/mock 套件 V0.1

机器标记：T3B_SUPPLIER_MOCK_KIT_READY
机器标记：NO_REAL_SECRET
机器标记：NO_PRODUCTION_VENDOR_CONTROL
验证命令：`npm run verify:t3b-supplier-mock-kit`
签名工具：`tools/t3b-open-platform-signature-helper.mjs`

## 1. 定位

本文档只用于 T3-B 供应商 SDK/mock 深化。它帮助第三方或内部联调人员理解开放平台签名、Webhook 信封、provider adapter mock 测试和事件查询的最小样例。

当前仍不接真实机器人、视频、设备、支付或人脸年龄核验供应商；不写真实密钥；不下发设备指令；不标记任何真实供应商生产就绪。

## 2. 可用范围

| 范围 | 本套件允许 | 本套件禁止 |
|---|---|---|
| 开放鉴权 | 生成 `/api/open/auth/token` 的 HMAC-SHA256 签名样例 | 在文档或代码中写入真实密钥 |
| Webhook | 生成 `/api/open/d/{endpoint}/webhooks/{provider}/{event}` 的签名样例 | 伪造真实供应商生产回调成功 |
| provider adapter | 使用 `/api/v1/integration/provider-adapters/{id}/test-mock` 做 mock 契约测试 | 标记真实供应商 production_ready |
| SDK 示例 | 给 JavaScript/Node.js 签名辅助工具 | 生成真实厂商 SDK 或真实控制命令 |

## 3. 签名 canonical

开放鉴权 canonical：

```text
app_key=<app_key>
nonce=<nonce>
timestamp=<timestamp>
```

Webhook canonical：

```text
endpoint_key=<endpoint_key>
event=<event>
idempotency_key=<idempotency_key>
payload_hash=<sha256(stable_json_payload)>
provider=<provider>
```

签名算法为 HMAC-SHA256 hex。签名材料必须从环境变量或凭证管理读取，不允许出现在命令行、日志、示例文件或 Git 仓库。

## 4. 本地 mock 签名命令

鉴权签名：

```bash
T3B_MOCK_SIGNING_KEY=mock-key-for-local-only node tools/t3b-open-platform-signature-helper.mjs auth --app-key p0-open-app --timestamp 1781424000000 --nonce nonce-t3b
```

Webhook 签名：

```bash
T3B_MOCK_SIGNING_KEY=mock-key-for-local-only node tools/t3b-open-platform-signature-helper.mjs webhook --endpoint-key p0-default --provider mock-provider --event payment.paid --idempotency-key idem-t3b-webhook --payload-json '{"event_id":"evt-t3b","idempotency_key":"idem-t3b-webhook","payload":{"status":"paid"}}'
```

> `mock-key-for-local-only` 只能用于本地示例。真实环境必须由 IT/安全在 credential ledger 中登记 credential_ref，并由运行环境注入。

## 5. 样例文件

| 文件 | 用途 |
|---|---|
| `docs/supplier-mock/t3b-open-auth-request.example.json` | 开放平台 token 请求样例 |
| `docs/supplier-mock/t3b-webhook-envelope.example.json` | Webhook 请求信封样例 |
| `docs/supplier-mock/t3b-provider-adapter-test.example.json` | provider adapter mock 测试请求样例 |

## 6. 验收标准

- `npm run verify:t3b-supplier-mock-kit` 输出 `T3B_SUPPLIER_MOCK_KIT_PASS`。
- 样例路径必须来自当前 OpenAPI。
- 样例签名只能是 `{{generated_hmac_sha256}}` 占位或由本地 mock 环境变量临时生成。
- 套件不得包含真实供应商 token、真实支付密钥、私钥或生产控制命令。
- T3-C 真实机器人/视频/设备控制仍保持 blocked，直到供应商 API/SDK、签名、沙箱、错误码和验收证据到位。

## 7. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-15 | V0.1 | 新增 T3-B SDK/mock 套件、签名工具和机器样例。 |
