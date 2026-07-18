# P0 任务-接口-权限-事件-验收映射表 V0.1

## 1. 文档定位

本文档用于把 P0 开发任务、API 契约、权限点、事件留痕和验收标准绑定在同一张执行表里。它解决的问题是：任务不能只说“完成接口”或“完成页面”，必须同时说明调用什么接口、需要什么权限、写什么事件、怎么验收。

本文档不替代 `p0-implementation-task-list.md`、`p0-api-contract-pack-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-operation-permission-matrix-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md` 和 `p0-end-to-end-acceptance-and-edge-cases-v0.1.md`；它负责把这些文档串起来。

## 2. 强制规则

1. P0 任务没有映射到接口、权限、事件和验收标准时，不得标记为“可验收完成”。
2. 数据库和运维类任务没有直接业务 API 时，必须映射到迁移、部署、恢复、监控或初始化验收。
3. 每个写接口必须声明幂等规则和成功/失败事件。
4. 高风险任务必须有审批、证据或复核链路。
5. 第三方供应商未确定时，仍必须完成统一接口、mock adapter、Webhook、轮询校验和补偿验收。
6. 权限点必须使用 `p0-operation-permission-matrix-v0.1.md` 的精确权限码，不得在可执行映射中使用 `*`、`view/manage`、`/review` 或同义旧码。
7. 客人 Token、设备凭证、开放应用凭证、Webhook 签名必须按 OpenAPI `x-auth-context` 校验，不得写成后台员工权限点。
8. 接口完成判定必须同时满足 `p0-openapi-operation-acceptance-matrix-v0.1.md` 的字段级请求体、权限、认证上下文、终端、幂等和事件要求，并能在 `p0-openapi-contract-closure-appendix-v0.1.md` 追溯到任务簇、权限、事件和样例准入要求。
9. 本文“主要接口”必须使用 OpenAPI 中的完整路径或明确的 `operationId`，不得使用 `/{id}`、`/settle`、`/copy`、`/stock-movements` 等相对路径简写；路径参数名必须与 OpenAPI 保持一致，例如 `{table_id}`、`{bill_id}`、`{order_id}`、`{device_id}`。
10. Web、Android、平板、一体机和供应商 mock 的正式联调代码必须先通过 `npm run verify:p0-contract`；生产调用只允许已进入 OpenAPI 的 `/api/v1/*` 和 `/api/open/*`，写接口请求字段必须与 OpenAPI requestBody schema 一致，operation 验收矩阵和 OpenAPI 追溯附录必须与 OpenAPI 同步。

## 3. P0 映射总表

| P0 任务/故事 | 主要接口 | 权限点 | 成功/失败事件 | 验收标准 |
|---|---|---|---|---|
| BE-000/BE-001 / US-0001 后端骨架 | `GET /api/v1/system/health`, `GET /api/v1/system/version` | `system.version.view` | `system.health_checked` | 服务可启动，健康检查正常，Maven 为唯一后端构建入口；BE-000 未完成前不得标记业务域正式并行开发完成 |
| BE-002/003 / US-0002 API 契约 | `/docs/openapi`, 全部 `/api/v1/*` | 按接口声明 `x-permission` 和 `x-auth-context` | `api.contract.generated` | OpenAPI 路径、字段级 requestBody、响应、错误码、幂等、Webhook 与 `p0-api-contract-pack` 和 operation 验收矩阵一致 |
| DB-001 至 DB-019 数据库迁移 | Flyway migration | 仅开发/运维权限 | `db.migration.applied` / `db.migration.failed` | 空库可重建；表、索引、约束、种子权限、主体绑定、应收应付、提成表可落地 |
| AUTH-001 登录会话 | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` | 登录前无；登录后按角色 | `auth.login.succeeded` / `auth.login.failed` / `auth.logout` | 员工和商户员工可登录、刷新、退出，失败留痕 |
| AUTH-002/003 角色权限和数据范围 | `/api/v1/access/roles`, `/api/v1/access/permissions` | `access.role.view`, `access.role.manage`, `access.permission.view` | `role.changed`, `permission.viewed` | 菜单、数据范围、商户隔离按权限生效 |
| AUTH-009 / US-0103 权限模板 | `/api/v1/access/permission-templates`, `/api/v1/access/permission-templates/{id}/copy`, `/api/v1/access/permission-templates/{id}/apply`, `/api/v1/access/temporary-grants`, `/api/v1/access/temporary-grants/{id}/revoke` | `access.permission_template.view`, `access.permission_template.manage`, `access.permission_template.apply`, `access.temporary_grant.create`, `access.temporary_grant.revoke` | `permission_template.created/copied/applied/revoked/expired` | 模板可复制、临时有效、单次消耗、撤销、显式拒绝优先 |
| GOV-001/002 / US-0102 四类主体绑定 | `/api/v1/platform/business-entity-bindings`, `/api/v1/platform/business-entity-bindings/{id}/change` | `entity.binding.view`, `entity.binding.manage` | `entity_binding.created`, `entity_binding.changed` | 同一经营对象同一时间只有一个 active 绑定，变更保留快照 |
| BE-006 / US-0101 商户空间建档 | `/api/v1/platform/merchants`, `/api/v1/platform/spaces`, `/api/v1/platform/contracts` | `merchant.profile.view`, `merchant.profile.manage`, `governance.space.view`, `governance.space.manage`, `contract.view`, `contract.draft.manage` | `merchant.updated`, `space.updated`, `contract.draft_saved` | 商户、门店、区域、点位、合同可维护且按数据范围隔离 |
| BE-007 / US-0104 终端开关 | `/api/v1/terminals/capability-switches`, `/api/v1/terminals/devices/bind`, `/api/v1/terminals/kiosks/{device_id}/config` | `terminal.switch.manage`, `device.mdm_policy.view` | `terminal_switch.changed`, `terminal.device.bound`, `terminal.access_rejected` | 端、模块、设备、版本开关生效；非售票型一体机不显示购票 |
| TKT-001/002 / US-0201 票价票种 | `/api/v1/ticketing/ticket-types`, `/api/v1/ticketing/price-rules`, `/api/v1/ticketing/ticket-types/{id}/publish` | `ticket.product.manage`, `ticket.price_variant.manage` | `ticket_product.changed`, `ticket_price.published` | 平日/周末/节假日基础价和派生/固定价可配置，发布需审批 |
| CUST-002/TKT-003 / US-0202 购票出票 | `/api/v1/customer/ticket-orders`, `/api/v1/ticketing/orders/{order_id}/issue`, `/api/v1/terminals/kiosks/{device_id}/config` | `x-auth-context=customer-token`, `ticket.order.create`, `ticket.ticket.issue` | `customer.ticket_order.created`, `ticket.issued`, `kiosk.ticket.order_created` | 客人端可自助购票；售票型一体机仅授权服务人员可辅助购票，非售票型不出现购票入口 |
| TKT-004/005/011 / US-0203 动态码核销 | `/api/v1/ticketing/tickets/{ticket_id}/credential`, `/api/v1/ticketing/verifications/preview`, `/api/v1/ticketing/verifications` | `ticket.credential.view`, `verification.execute` | `ticket_credential.refreshed`, `verification.succeeded`, `verification.failed` | 在线校验签名、有效期、随机串、权益状态；截图/过期/伪造拒绝 |
| TKT-008/009 / US-0204 退款冲正 | `/api/v1/ticketing/refunds`, `/api/v1/ticketing/refunds/{refund_id}/approve`, `/api/v1/finance/refunds` | `ticket.refund.request`, `ticket.refund.approve`, `finance.refund.create` | `ticket_refund.requested`, `ticket_refund.approved`, `finance_refund.created` | 退款不删除原核销，只生成冲正、退款和财务对账记录 |
| CUST-001/CMP-002 同意和客人身份 | `/api/v1/customer/auth/phone-login`, `/api/v1/customer/consents` | `public` / `x-auth-context=customer-token` | `customer.auth.login`, `customer.consent.created`, `consent.withdrawn` | 手机号统一身份；收集个人信息前必须有同意记录 |
| CUST-005 / US-0502 客人积分摘要 | `/api/v1/customer/points` / `getCustomerPoints` | `x-auth-context=customer-token` | `customer.points_viewed`, `customer.points_view_failed` | P0 只展示积分余额、会员等级和最近积分摘要；积分明细、礼物列表、兑换发起和兑换记录进入 P1 |
| BAR-009 / US-0301 订台和卡座入场 | `/api/v1/customer/bar-reservation-options`, `/api/v1/customer/bar-reservations`, `/api/v1/bar/reservations`, `/api/v1/bar/reservations/{id}/check-in-guest` | `x-auth-context=customer-token`, `bar.table.guest_checkin`, `bar.table_sales.attribute` | `customer.bar_reservation_options_viewed`, `customer.bar_reservation.created`, `bar.table_guest.checked_in`, `bar.table_guest.rejected`, `order.sales_attributed` | 客人端可读取可订酒吧/卡座/时段/定金规则；卡座免通票人数只限预定酒吧，额度并发只成功一笔 |
| BAR-011 / US-0302 年龄人脸核验 | `/api/v1/bar/age-verifications` | `bar.entry.age_verify`, `bar.entry.face_result.write` | `bar.age_verification.requested/passed/rejected/blocked` | 生产未配置供应商或等效设备时不得标记正式通过 |
| BAR-001 至 BAR-004 开台点单账单 | `/api/v1/bar/tables/{table_id}/open`, `/api/v1/bar/orders`, `/api/v1/bar/bills/{bill_id}/settle` | `bar.table.open_close`, `bar.order.create` | `bar.table.opened`, `bar.order_created`, `bar.bill.settled` | 开台、点单、账单状态变化可追溯 |
| BAR-010 / US-0303 低消支付 | `/api/v1/bar/bills/{bill_id}/pay-intents`, `/api/v1/bar/bills/{bill_id}/settle`, `/api/v1/finance/payment-intents` | `finance.payment_intent.create`, `finance.offline_payment.confirm` | `payment.gateway_notified`, `finance_payment.registered`, `bar.bill.settled` | 低消只按酒吧账单消费计算，票务/卡座权益不得抵扣 |
| BAR-007/SUP-004 / US-0304 出酒交接库存扣减 | `/api/v1/inventory/trace-codes/scan-handover`, `/api/v1/inventory/stock-movements`, `/api/v1/bar/orders/{order_id}/deliver` | `inventory.stock.move`, `bar.order.create` | `inventory.outbound_verified`, `bar.order_delivered`, `operation_handover.created` | 高价值酒水必须在线扣减和交接留痕，失败进入异常 |
| SUP-005 / US-0305 存酒取酒 | `/api/v1/inventory/saved-bottles`, `/api/v1/inventory/saved-bottles/{id}/claim` | `bar.saved_bottle.manage` | `bar.saved_bottle.created`, `bar.saved_bottle_picked_up`, `bar.saved_bottle.voided` | 存酒登记、领取、作废有同意、照片/签收和状态 |
| SUP-001 至 SUP-006 供应链库存 | `/api/v1/inventory/suppliers`, `/api/v1/inventory/purchase-requests`, `/api/v1/inventory/stock-in`, `/api/v1/inventory/stock-adjustments` | `inventory.supplier.view`, `inventory.supplier.manage`, `inventory.purchase_request.view`, `inventory.purchase_request.manage`, `inventory.stock.move`, `inventory.stock.adjust` | `inventory.inbound_confirmed`, `inventory.counted`, `inventory.damaged` | 供应商、采购、入库、盘点、报损、报溢均有审批和证据 |
| AMU-001 至 AMU-007 / US-0306 游乐核销和安全巡检 | `/api/v1/amusement/projects`, `/api/v1/amusement/sessions`, `/api/v1/amusement/verifications`, `/api/v1/amusement/projects/{id}/open-eligibility`, `/api/v1/amusement/safety-templates`, `/api/v1/amusement/safety-inspections`, `/api/v1/amusement/safety-inspections/{id}/submit`, `/api/v1/amusement/safety-inspections/{id}/rectify`, `/api/v1/amusement/safety-inspections/{id}/recheck`, `/api/v1/amusement/safety-inspections/{id}/approve-reopen` | `amusement.session.view`, `amusement.session.manage`, `amusement.verification.execute`, `amusement.verification.override`, `amusement.safety_template.view`, `amusement.safety_template.manage`, `amusement.safety_inspection.view`, `amusement.safety_inspection.create`, `amusement.safety_inspection.submit`, `amusement.safety_inspection.rectify`, `amusement.safety_inspection.recheck`, `amusement.safety_inspection.approve_reopen` | `amusement.safety_inspection.passed/blocked/rechecked/reopened`, `amusement.verification_succeeded/failed` | AC-AMU-01 至 AC-AMU-05；未通过开放前安全巡检不得开放；阻断项必须整改复检并主管恢复；容量已满不得核销 |
| FIN-001 / US-0401 应收应付 | `/api/v1/finance/ar-ap-items`, `/api/v1/finance/ar-ap-items/{id}/payments` | `finance.ar_ap.manage`, `finance.payment.register` | `finance_ar_ap.created`, `finance_payment.registered` | 台账绑定经营对象、运营主体、合同主体、结算主体和来源单据 |
| FIN-002/004 / US-0402 日结和对账差异 | `/api/v1/finance/daily-closes`, `/api/v1/finance/reconciliation/differences` | `finance.daily_close.lock`, `finance.reconciliation.view` | `finance_daily_close.locked`, `finance_reconciliation.difference_found` | 日结锁定后不能直接改金额，差异进入补偿/复核 |
| DB-019/BAR-012/FIN-005 / US-0403 卡座提成 | `/api/v1/bar/bills/{bill_id}/commissions/calculate`, `/api/v1/finance/ar-ap-items` | `bar.table_commission.calculate`, `bar.table_commission.view`, `bar.table_commission.review`, `finance.ar_ap.manage` | `bar.table_commission.calculated`, `bar.table_commission.adjusted` | 销售人员 15%，非销售人员 10%，退款/免单后冲正不覆盖 |
| AUTH-004 至 AUTH-008 / 第三方开放 | `/api/open/auth/token`, `/api/open/d/{endpoint_key}/...` | 开放应用凭证、Webhook 签名、`openplatform.app.manage`, `openplatform.credential.rotate`, `openplatform.mapping.manage`, `openplatform.reconcile.execute` | `open_auth.succeeded/failed`, `api_call.logged` | 动态 endpoint、签名、nonce、防重放、失败锁定可用 |
| INT-001 至 INT-008 / US-0404 供应商 Adapter | `/api/v1/integration/provider-adapters`, `/api/v1/integration/provider-adapters/{id}/test-mock`, `/api/v1/integration/provider-adapters/{id}/mark-production-ready`, Webhook | `integration.provider_adapter.view`, `integration.provider_adapter.create`, `integration.provider_adapter.update`, `integration.provider_adapter.test`, `integration.provider_adapter.production_ready`, `openplatform.mapping.manage` | `open_mapping.changed`, `open_compensation.executed`, `provider_adapter.tested` | 供应商未定可 mock；Webhook 重复幂等、漏发轮询补偿 |
| DATA-001 至 DATA-007 / US-0405 风控看板 | `/api/v1/analytics/dashboards/p0-overview`, `/api/v1/analytics/risk-alerts`, `/api/v1/evidence/events` | `analytics.dashboard.view`, `analytics.risk_alert.view`, `evidence.event.view` | `risk_alert.created`, `sensitive.viewed`, `evidence.exported` | 看板能追溯风险来源事件和复核状态 |
| AND-001 至 AND-012 Android 终端 | 复用核销、设备、库存、酒吧接口；客户端路径只允许已进入 OpenAPI 的 `/api/v1/*` 或 `/api/open/*` | 设备绑定 + 员工权限 | `terminal.device.bound`, `verification.succeeded/failed`, `operation_handover.created` | 工作手机/平板按终端开关和 MDM 状态执行，断网不得高风险离线成功；`npm run verify:p0-contract` 通过 |
| FE-001 至 FE-018 管理后台 | 复用全部后台 API；客户端路径只允许已进入 OpenAPI 的 `/api/v1/*` 或 `/api/open/*` | 按菜单/页面权限 | 页面操作对应业务事件 | 菜单随权限显示，页面写操作都能在事件中心追溯；`npm run verify:p0-contract` 通过 |
| OPS-001 至 OPS-009 / US-0406 160 运维 | 健康检查、备份脚本、恢复演练记录 | IT/运维权限 | `deployment.released`, `backup.completed`, `restore.drill_completed` | 可发布、回滚、NAS 备份、恢复演练、密钥台账 |
| OPS-001 至 OPS-009 / US-0503 生产前置责任状态 | `GET /api/v1/ops/production-readiness/items` / `listProductionReadinessItems`, `PUT /api/v1/ops/production-readiness/items/{id}` / `updateProductionReadinessItem`, `POST /api/v1/ops/production-readiness/checks` / `createProductionReadinessCheck`；Runbook 引用 `mdm-android-enterprise-implementation-plan-v0.1.md`、`production-credential-rotation-runbook-v0.1.md`、`nas-backup-and-restore-runbook-v0.1.md` | `ops.production_readiness.view`, `ops.production_readiness.manage`, `ops.production_readiness.check` | `production_readiness.viewed`, `production_readiness.updated`, `production_readiness.checked`, `mdm.policy_checked`, `credential.rotation_checked`, `restore.drill_checked` | P0 做轻量系统台账/API；任一前置项未通过时不得标记真实生产就绪；必须显示阻断项、责任部门、计划日期和降级方式 |
| QA-001 至 QA-008 验收 | 全部 P0 API 和页面 | 测试账号最小授权 | `qa.test_executed` | 测试数据、接口自动化、端到端、权限、留痕和边界用例全部通过 |

## 4. 阻断规则

| 缺口 | 处理 |
|---|---|
| 任务有接口但无权限或认证上下文 | 不得进入联调，先补权限矩阵、OpenAPI `x-permission`/`x-auth-context` 和 operation 验收矩阵 |
| 任务有写操作但无事件 | 不得通过验收，先补成功/失败事件 |
| 任务有金额影响但无财务台账或日结规则 | 不得标记财务闭环完成 |
| 任务依赖供应商但无 mock adapter | 不得被供应商未定阻塞 P0，但也不得标记生产就绪 |
| 任务涉及高价值酒水/退款/人工放行/敏感证据但无审批 | 不得上线 |
| Android/一体机任务未绑定终端开关 | 不得进入现场演示 |

## 5. 验收标准

### AC-MAP-001 任务可追溯

Given 任一 P0 开发任务准备验收，  
When 测试人员查本文档，  
Then 必须能找到对应接口、权限、事件和验收标准。

### AC-MAP-002 接口和权限一致

Given 后端输出 OpenAPI，  
When 检查任一写接口，  
Then OpenAPI 中的字段级请求体、权限、认证上下文、终端、幂等、事件声明必须与本文档、权限矩阵和 operation 验收矩阵一致。

Given 测试人员按本文生成接口自动化用例，  
When 读取任一“主要接口”单元格，  
Then 其中的路径必须能在 `openapi/openapi-p0.json` 或 `openapi/openapi-p0.yaml` 中直接匹配，或能通过同一行标注的 `operationId` 精确定位。

### AC-MAP-003 事件和证据一致

Given 任一高风险业务动作完成，  
When 审计人员查询事件中心，  
Then 必须能按本文事件名查到操作者、终端、业务对象、结果、原因和证据引用。

### AC-MAP-004 验收不可跳表

Given 某任务代码、页面或数据库已完成，  
When 它未通过本文对应的权限、事件和端到端验收，  
Then 不得标记为 P0 完成。

## 6. 后续维护规则

1. 新增 P0 任务时，必须同步新增本文映射行。
2. 修改 API 路径时，必须同步修改 `p0-api-contract-pack-v0.1.md`、`p0-openapi-operation-acceptance-matrix-v0.1.md`、`p0-openapi-contract-closure-appendix-v0.1.md` 和本文。
3. 修改权限点时，必须同步修改 OpenAPI、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-operation-permission-matrix-v0.1.md` 和本文。
4. 修改事件名时，必须同步修改 OpenAPI、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md` 和本文。
5. 修改验收标准时，必须同步修改 `p0-end-to-end-acceptance-and-edge-cases-v0.1.md` 或对应专项验收文档。

## 7. 变更日志

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-06-10 | V0.1 | 新增 P0 任务-接口-权限-事件-验收映射表，覆盖地基、平台治理、票务、客人端、酒吧、库存、游乐、财务、第三方、风控、终端、运维和测试 |
| 2026-06-12 | V0.1 可编码收口修复 | 将主要接口统一修正为 OpenAPI exact path 或明确 operationId；补充禁止相对路径简写规则；同步 BE-000/BE-001 后端准入映射 |
| 2026-06-12 | V0.1 客户端路径闸口 | 强制 Web/Android/一体机/供应商 mock 正式联调代码通过 `npm run verify:client-routes`；同步 FE-018 和 AND-012 映射 |
| 2026-06-12 | V0.1 客户端 OpenAPI 覆盖闸口 | 强制客户端正式路径和 HTTP 方法同时通过 `npm run verify:client-openapi`；未入 OpenAPI 的路径或方法不得作为 P0 联调依据 |
| 2026-06-12 | V0.1 客户端请求字段闸口 | 强制客户端正式写接口通过 `npm run verify:client-payload`；防止路径正确但请求字段与 OpenAPI schema 不一致 |
| 2026-06-12 | V0.1 operation 矩阵同步闸口 | 强制 `p0-openapi-operation-acceptance-matrix-v0.1.md` 通过 `npm run verify:operation-matrix`；防止 OpenAPI 已改但逐接口验收矩阵未同步 |
| 2026-06-13 | V0.1 OpenAPI 追溯附录闸口 | 强制 `p0-openapi-contract-closure-appendix-v0.1.md` 通过 `npm run verify:openapi-closure`；正文保留任务组映射，逐 operation 权限、事件、任务簇、终端和样例准入以附录为准 |
| 2026-06-13 | V0.1 高风险映射与验证分层 | 新增 `CUST-005 / US-0502` 和 `US-0503` 映射；将 Web、Android、平板、一体机和供应商 mock 的正式联调准入口径收口为 `npm run verify:p0-contract` |
| 2026-06-13 | V0.1 生产前置台账接口收口 | 将 US-0503 从纯 Runbook/人工台账补成 P0 轻量系统台账 API；修复客人积分事件名与 OpenAPI 不一致问题 |
