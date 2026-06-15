# P1-CONTRACT-COMPLIANCE-EXPIRY-01 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the contract key date and compliance expiry reminder backend loop so operations can record contract dates, licenses, qualifications, insurance items, scan expiry risk, and keep audit evidence.

**Architecture:** Add an independent `compliance` Spring package with records, DTOs, service, repository, controller, and JDBC repository. The slice stores reminders and blocking suggestions only; it does not automatically change merchant, contract, amusement, inventory, or finance states.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, Maven, existing `ApiResponse`, `PageResponse`, `IdempotencyService`, `AuditEventService`, and permission guard.

---

## 用户故事地图

| 版本层 | 用户故事 |
| --- | --- |
| MVP/P1 当前切片 | 作为平台招商/法务/运营，我可以登记合同关键日期，绑定商户、责任人、提醒天数和附件证据 |
| MVP/P1 当前切片 | 作为运营/合规负责人，我可以登记证照、资质、保险到期台账，设置提醒或阻断建议 |
| MVP/P1 当前切片 | 作为运营主管，我可以执行到期扫描，看到临期、过期和建议阻断事项，并保留扫描事件 |
| 第二期 | OCR/本地 AI 识别合同或证照文本后生成待人工确认建议 |
| 未来版本 | 电子签章、保险理赔闭环、特种设备外部校验证书联网核验、自动发起整改/审批 |

## 文件结构

- 新增 `backend/src/main/java/com/complex/platform/compliance/*`：记录、服务、仓储、Controller。
- 新增 `backend/src/main/java/com/complex/platform/compliance/dto/*`：请求 DTO。
- 新增 `backend/src/main/resources/db/migration/V028__p1_contract_compliance_expiry.sql`：合同关键日期、到期事项、提醒结果和权限。
- 新增 `backend/src/test/java/com/complex/platform/compliance/*Tests.java`：Service、Controller、JDBC 集成测试。
- 新增 `docs/p1-contract-compliance-expiry-api-contract-v0.1.md`：P1 API 合同。

## 数据结构 / 接口变化

- 表 `contract_key_dates`：合同关键日期，含 `contract_id`、`merchant_id`、`date_type`、`due_date`、`reminder_days`、`source_type`、`human_confirmed`、`evidence_attachment_ids`。
- 表 `compliance_expiry_items`：证照/资质/保险到期台账，含 `subject_type`、`subject_id`、`merchant_id`、`item_type`、`valid_to`、`expiry_action`、`status`、`evidence_attachment_ids`。
- 表 `compliance_reminders`：扫描生成的临期/过期提醒，含 `source_type`、`source_id`、`reminder_type`、`severity`、`blocking_suggested`、`message`。
- API：
  - `GET /api/v1/compliance/contract-key-dates`
  - `POST /api/v1/compliance/contract-key-dates`
  - `GET /api/v1/compliance/expiry-items`
  - `POST /api/v1/compliance/expiry-items`
  - `GET /api/v1/compliance/reminders`
  - `POST /api/v1/compliance/reminders/scan`

## 测试点

- 合同关键日期必填字段校验，失败写审计事件。
- 合同关键日期创建幂等，重复请求不重复写入。
- 证照/资质/保险到期事项创建，支持 `remind_only`、`block_operation`、`supervisor_approval`。
- 到期扫描生成临期和过期提醒，并根据阻断策略给出 `blocking_suggested`。
- Controller 校验认证、权限和写接口幂等键。
- JDBC 集成测试验证 V028 表、权限、角色授权和读写映射。

## 验收标准

- focused tests 通过：`ComplianceServiceTests`、`ComplianceControllerTests`、`ComplianceJdbcIntegrationTests`。
- `npm run verify:p0-contract` 通过。
- focused Semgrep 对新增 compliance 包和 V028 迁移无发现。
- 160 上 `mvn test` 全量通过。

## 风险与回滚

- 风险：提醒被误解为自动阻断。控制方式：字段命名为 `blocking_suggested`，服务只生成建议，不改其它业务状态。
- 风险：AI 识别未落地导致输入口径不完整。控制方式：本切片只保存 `source_type=ai_suggestion` 和 `human_confirmed`，真实 AI 识别后续接入。
- 回滚：删除 V028 迁移、新增 `compliance` 包、测试和 P1 API 合同；已应用数据库需用后续迁移归档权限和表，而不是直接删生产表。

## 执行清单

- [x] 写入 Service 失败测试并确认失败
- [x] 实现记录、DTO、Repository、Service 并确认 Service 测试通过
- [x] 写入 Controller 失败测试并确认失败
- [x] 实现 Controller 并确认 Controller 测试通过
- [x] 写入 JDBC 失败测试并确认失败
- [x] 新增 V028 迁移并确认 JDBC 测试通过
- [x] 补充 P1 API 合同文档
- [x] 运行 focused tests、P0 契约、Semgrep、全量测试
