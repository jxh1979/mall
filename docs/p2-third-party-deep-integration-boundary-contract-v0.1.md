# P2-E 第三方深度联动边界契约 V0.1

## 1. 定位

P2-E 覆盖机器人平台、视频平台、设备厂商、人脸/年龄核验等供应商深度联动。当前系统已经具备通用开放平台和供应商适配器能力，本期不硬编码任何具体厂商 SDK。

## 2. 已有系统底座

| 能力 | 已有入口 |
| --- | --- |
| 供应商适配器登记 | `GET/POST/PUT /api/v1/integration/provider-adapters` |
| 适配器生产就绪标记 | `POST /api/v1/integration/provider-adapters/{id}/mark-production-ready` |
| 模拟测试 | `POST /api/v1/integration/provider-adapters/{id}/test-mock` |
| 开放平台发 token | `POST /api/open/auth/token` |
| 接收第三方 Webhook | `POST /api/open/d/{endpoint_key}/webhooks/{provider_code}/{event_type}` |
| 第三方查询事件结果 | `GET /api/open/d/{endpoint_key}/events/{event_id}` |
| 重试任务 | `/api/v1/integration/retry-tasks` |
| 补偿任务 | `/api/v1/integration/compensation-tasks` |
| 对账批次/差异 | `/api/v1/integration/reconciliation/*` |

## 3. 适配器类型

当前通用适配器类型已覆盖：

- `payment`
- `ticket_channel`
- `face_age`
- `wecom`
- `robot`
- `video`
- `device`
- `retail`
- `catering`

## 4. 供应商资料到位后必须补齐

| 资料 | 用途 | 没有时是否允许生产启用 |
| --- | --- | --- |
| API/SDK 文档 | 字段映射和调用方式 | 不允许 |
| 签名/验签规则 | 鉴权和防篡改 | 不允许 |
| Webhook 事件样例 | 事件接收、幂等、补偿 | 不允许 |
| 轮询接口与频率限制 | Webhook 失败后的校验 | 不允许 |
| 错误码和重试语义 | 失败补偿与告警 | 不允许 |
| 沙箱账号 | 联调验证 | 不允许 |
| 生产账号与密钥引用 | 生产调用 | 不允许明文，只能存 `credential_ref` |
| 数据保存期限与调阅规则 | 视频、人脸、设备日志合规 | 不允许缺失 |

## 5. 接入策略

- 默认采用 Webhook + 定时轮询校验。
- Webhook 负责实时事件进入。
- 定时轮询负责发现漏通知、状态不一致和对账差异。
- 所有第三方事件必须落事件台账，保留幂等键、供应商事件 ID、处理结果和错误信息。
- 所有密钥只保存引用，不保存明文。
- 没有真实供应商资料前，只允许 `mock` 或 `sandbox`，不允许 `production_ready`。

## 6. 禁止事项

- 禁止用伪造字段假装某厂商已完成接入。
- 禁止把机器人、视频、人脸、设备厂商的特定 SDK 写死到核心业务模块。
- 禁止把供应商密钥写入代码、配置样例或文档正文。
- 禁止在没有合规审查和采购确认前启用人脸/年龄正式核验生产能力。

## 7. 验收标准

供应商正式进入开发时，必须逐项提供：

- 字段映射表。
- Webhook 样例和签名样例。
- 轮询策略。
- 对账口径。
- 异常补偿策略。
- 沙箱联调证据。
- 生产 credential_ref。
- 生产就绪审批留痕。
