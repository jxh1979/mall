# P1 游乐安全告知、电子免责与事故工单 API 契约 V0.12

文档类型：Implementation / API Contract

本文档固化 P1-AMUSEMENT-SAFETY-01「游乐安全告知/电子免责、事故工单」的最小可编码契约。P0 安全巡检仍是开放前阻断的唯一主口径；本 P1 模块负责游客/监护人告知确认、免责留痕和事故处理闭环。

## 1. 范围

| 能力 | P1 本次包含 | 本次不包含 |
|---|---|---|
| 安全告知 | 创建、查询项目级安全告知/风险披露/免责文本 | 富文本编辑器、法务版本审批流 |
| 电子免责 | 记录游客或监护人确认、签署方式、版本、证据附件引用 | 第三方电子签 SDK、实名核身 |
| 事故工单 | 创建、处理、关闭事故记录，保留动作流水 | 保险理赔自动化、客服赔付审批 |
| 证据 | 保存附件 ID、图片/视频片段引用 ID | 平台内视频播放、自动视频绑定 |
| 留痕 | 所有写操作幂等并写审计事件 | AI 事故定责 |

## 2. 接口

| Method | Path | Permission | 幂等 | 说明 |
|---|---|---|---|---|
| `GET` | `/api/v1/amusement/safety-notices` | `amusement.safety_notice.view` | 否 | 查询安全告知 |
| `POST` | `/api/v1/amusement/safety-notices` | `amusement.safety_notice.manage` | 是 | 创建安全告知 |
| `POST` | `/api/v1/amusement/safety-notices/{notice_id}/acknowledgements` | `amusement.waiver.acknowledge` | 是 | 记录电子免责/告知确认 |
| `GET` | `/api/v1/amusement/incidents` | `amusement.incident.view` | 否 | 查询事故工单 |
| `POST` | `/api/v1/amusement/incidents` | `amusement.incident.create` | 是 | 创建事故工单 |
| `POST` | `/api/v1/amusement/incidents/{id}/actions` | `amusement.incident.process` | 是 | 追加事故处理动作 |
| `POST` | `/api/v1/amusement/incidents/{id}/close` | `amusement.incident.close` | 是 | 关闭事故工单 |

## 3. 状态与枚举

| 对象 | 字段 | 枚举 |
|---|---|---|
| 安全告知 | `notice_type` | `safety_notice`/`waiver`/`risk_disclosure` |
| 安全告知 | `status` | `active`/`inactive`/`archived` |
| 事故 | `severity` | `low`/`medium`/`high`/`critical` |
| 事故 | `status` | `created`/`processing`/`pending_external`/`resolved`/`closed`/`cancelled` |
| 事故动作 | `action_type` | `created`/`commented`/`status_changed`/`closed` |

规则：`closed` 与 `cancelled` 为终态，终态后不得继续处理或关闭。

## 4. 字段

### 4.1 创建安全告知

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一创建请求幂等 |
| `evidence_attachment_ids` | 否 | string[] | 附件/法务材料引用 |
| `project_id` | 是 | string | 所属综合体/项目 |
| `amusement_project_id` | 否 | string | 游乐项目 ID |
| `notice_type` | 是 | enum | 安全告知、免责或风险披露 |
| `title` | 是 | string | 1-160 字符 |
| `content_version` | 是 | string | 文本版本 |
| `content_text` | 是 | string | 告知正文 |
| `status` | 是 | enum | `active`/`inactive`/`archived` |
| `effective_from` | 否 | instant | 生效时间 |
| `effective_to` | 否 | instant | 失效时间 |
| `operator_user_id` | 是 | string | 操作人 |

### 4.2 电子免责/告知确认

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一确认请求幂等 |
| `evidence_attachment_ids` | 否 | string[] | 签名、照片、录屏等证据引用 |
| `project_id` | 是 | string | 所属项目 |
| `amusement_project_id` | 否 | string | 游乐项目 |
| `session_id` | 否 | string | 场次 |
| `customer_id` | 是 | string | 客户/游客 ID |
| `visitor_name` | 是 | string | 游客姓名或现场登记名 |
| `signer_name` | 是 | string | 签署人；未成年人可为监护人 |
| `acknowledgement_method` | 是 | string | 如 `onsite_tablet`、`staff_phone`、`miniapp` |
| `content_version` | 是 | string | 确认的文本版本 |
| `content_hash` | 否 | string | 文本哈希，占位给后续法务/电子签 |
| `acknowledged_at` | 否 | instant | 为空时取服务端当前时间 |
| `operator_user_id` | 是 | string | 操作人 |

### 4.3 创建事故工单

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一创建请求幂等 |
| `operator_note` | 否 | string | 创建说明 |
| `evidence_attachment_ids` | 否 | string[] | 事故照片/视频/报告引用 |
| `project_id` | 是 | string | 所属项目 |
| `amusement_project_id` | 否 | string | 游乐项目 |
| `session_id` | 否 | string | 场次 |
| `device_id` | 否 | string | 设备 |
| `customer_id` | 否 | string | 游客 |
| `severity` | 是 | enum | 严重级别 |
| `title` | 是 | string | 事故标题 |
| `description` | 是 | string | 事故描述 |
| `reported_by_user_id` | 是 | string | 上报人 |
| `reported_channel` | 是 | string | 上报渠道 |
| `occurred_at` | 否 | instant | 为空时取服务端当前时间 |

### 4.4 处理和关闭事故

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一动作幂等 |
| `operator_note` | 处理必填，关闭可选 | string | 处理说明 |
| `evidence_attachment_ids` | 否 | string[] | 证据引用 |
| `target_status` | 处理必填 | enum | `processing`/`pending_external`/`resolved` |
| `operator_user_id` | 处理必填 | string | 处理人 |
| `resolution` | 关闭必填 | string | 结论和处理结果 |
| `closed_by_user_id` | 关闭必填 | string | 关闭人 |

## 5. 事件

| 动作 | 成功事件 | 失败事件 |
|---|---|---|
| 查询安全告知 | `amusement.safety_notice.list` | 暂不单列 |
| 创建安全告知 | `amusement.safety_notice.create` | `amusement.safety_notice.create_failed` |
| 电子免责确认 | `amusement.waiver.acknowledge` | `amusement.waiver.acknowledge_failed` |
| 查询事故 | `amusement.incident.list` | 暂不单列 |
| 创建事故 | `amusement.incident.create` | `amusement.incident.create_failed` |
| 处理事故 | `amusement.incident.action` | `amusement.incident.action_failed` |
| 关闭事故 | `amusement.incident.close` | `amusement.incident.close_failed` |

## 6. 错误处理矩阵

| 错误 | 响应码 | 业务码 | 处理 |
|---|---|---|---|
| 缺少幂等键 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 必填字段缺失 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 枚举非法 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 安全告知不存在 | 404 | `SAFETY_NOTICE_NOT_FOUND` | 拒绝确认 |
| 事故不存在 | 404 | `INCIDENT_NOT_FOUND` | 拒绝处理 |
| 事故已关闭 | 409 | `INCIDENT_CLOSED` | 拒绝继续处理 |
| 幂等冲突 | 409 | `IDEMPOTENCY_CONFLICT` | 要求换 key 或查询原请求 |

## 7. Test Case Specifications

| Test ID | 类型 | 验收点 |
|---|---|---|
| AMU-P1-UT-001 | Unit | 创建安全告知缺少项目返回 `VALIDATION_FAILED` 并写失败审计 |
| AMU-P1-UT-002 | Unit | 创建安全告知成功写入记录和审计事件 |
| AMU-P1-UT-003 | Unit | 对不存在的安全告知确认免责返回 `SAFETY_NOTICE_NOT_FOUND` |
| AMU-P1-UT-004 | Unit | 免责确认保存签署人、文本版本、确认方式和证据引用 |
| AMU-P1-UT-005 | Unit | 事故创建写初始动作流水 |
| AMU-P1-UT-006 | Unit | 事故处理可变更到处理/外部待办/已解决 |
| AMU-P1-UT-007 | Unit | 事故关闭后继续处理返回 `INCIDENT_CLOSED` |
| AMU-P1-MVC-001 | Controller | 7 条接口均校验精确权限 |
| AMU-P1-IT-001 | Integration | V024 创建表、索引、权限和平台管理员授权 |
| AMU-P1-IT-002 | Integration | JDBC 能保存并读取安全告知、免责确认、事故和动作 |

## 8. References

| Topic | Location |
|---|---|
| 本切片计划 | `docs/superpowers/plans/2026-06-14-p1-amusement-safety-waiver-incident.md` |
| P1 路线 | `docs/master-roadmap.md#阶段-8-市场基准增强与-saas-化深度能力` |
| 游乐 P0/P1 边界 | `docs/amusement-p0-safety-inspection-spec-v0.1.md` |
| 游乐开发设计 | `docs/amusement-operations-development-design.md` |
