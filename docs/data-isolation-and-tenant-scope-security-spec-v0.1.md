# 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.1

> 创建日期：2026-06-18
> 状态：权威安全规格（补齐此前缺口——隔离模型此前仅存在于各迁移 .sql 注释中，无单一权威文档）。
> 适用范围：开发、测试、安全、验收。当前迁移 scope 基线以 §3A 自动生成区块为准；历史人工清单仅作解释背景，不再作为迁移版本上限。
> 关联：盘点表 `docs/domain-ownership-scope-audit-v0.1.csv`（逐表 scope 映射，本规格正式引用并解释它）；越权必测口径 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`（"数据范围穿透"）、`docs/full-admin-ui-acceptance-matrix-v0.1.md`（UI-ADM-MER-002）。

机器识别标记：DATA_ISOLATION_SPEC_READY、TENANT_SCOPE_INJECTION_FROM_TOKEN_ONLY、SCOPE_AUDIT_CSV_AUTHORITATIVE_LINK。

> 计数漂移修订（2026-06-21）：历史补登中的 V061 也是快照。当前真实迁移覆盖以 §3A 自动迁移隔离基线为准，由 `npm run docs:data-isolation-spec` 生成、`npm run verify:data-isolation-spec` 校验。

## 1. 隔离边界与 scope 语义

| scope | 含义 | 来源 | 层级 |
|---|---|---|---|
| `organization_id` | 组织/集团（角色归属）。NULL = 平台级全局角色 | 安全 token | 最高 |
| `merchant_id` | 商户（经营主体）。绝大多数业务表的隔离键 | 安全 token | 中 |
| `project_id` | 综合体/项目（如平面图） | 安全 token / 继承 | 中 |
| `venue_id` / `bar_venue_id` / `space_id` | 场地/酒吧/点位（继承到 merchant） | 继承 | 低 |

scope 类型（与盘点 CSV 的 `scope_type` 对齐）：
- `direct`：表自带 scope 列（如 `merchant_id`），直接过滤。
- `inherited`：无自带列，通过外键 join 到带 scope 列的父表过滤。
- `single_complex_default`：单综合体默认全局可见，多综合体启用时再收紧。

## 2. 权威约定：scope 注入只能来自安全 token（**绝不取请求参数**）

迁移 V044/V046 的 .sql 注释已确立、本规格正式提升为强制条款：

> 隔离列（`merchant_id`/`organization_id`/`project_id`）的值**在写入时由调用方的安全 token 注入**，**绝不允许从请求体或查询参数读取**。读取查询必须由服务端按 token 的 scope 强制追加过滤条件，不依赖前端传参。

- 写入：Service 从 `AuthorizationService.currentMerchantScope()` / `currentActor()` 取 scope 落库。
- 读取：repository 提供 `listXxxByMerchant(scope)` 等按 scope 过滤的方法；平台级（scope 为空）才走全量 `listXxx()`。参考实现：`FinanceService.listArApItems()`（`currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems`）。
- 旧行兼容：隔离列均为 **nullable**（`ADD COLUMN IF NOT EXISTS … VARCHAR`），存量旧行 `merchant_id IS NULL`；部分索引 `WHERE merchant_id IS NOT NULL` 只对已落 scope 的行建索引。**回填策略**见 §5。

## 3. 历史人工隔离列清单（初版快照，仅作背景）

| 迁移 | 表 | 列 | 类型 | 索引/约束 |
|---|---|---|---|---|
| V044 | `ar_ap_items` | merchant_id | VARCHAR(64) | 部分索引；另加 `uq_daily_close_scope_date UNIQUE(business_date,scope_type,scope_id)` on finance_daily_closes |
| V046 | inventory 10 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
| V047 | bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills | merchant_id | VARCHAR(100) | 各部分索引 |
| V048 | `roles` | organization_id | VARCHAR(64) | 部分索引（NULL=平台全局角色）|
| V049 | amusement 4 表 | merchant_id | VARCHAR(?) | 部分索引已补（V065，ISO-GAP-03 已关闭）|
| V051 | `compliance_reminders` | merchant_id | VARCHAR(64) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
| V052 | `mall_floor_plans` | merchant_id | VARCHAR(64) | 部分索引 |
| V055 | `staff_level_assignments` | organization_id + merchant_id | VARCHAR(64) | 两个部分索引（org/merchant）；`staff_levels` 为平台全局参考数据 `single_complex_default`，无 scope 列 |
| V057 | `supervisor_review_decisions` | （无 scope 列） | — | 本次新增 `approval_level` / `required_approver_rank` 仅为审批留痕字段，**非隔离键**；该表仍无 organization/merchant scope 列（隔离裁决见 §6 / §7 注） |
| V059 | `business_process_instances` | merchant_id + project_id | VARCHAR(64) | `direct`；商户+综合体双键直接过滤 |
| V059 | `business_process_step_records` | （无自带列） | — | `inherited`：经 `process_instance_id` 外键 join `business_process_instances` 继承 merchant_id/project_id 过滤 |
| V059 | `service_responsibility_records` | merchant_id | VARCHAR(64) | `direct`；商户级直接过滤 |
| V060 | `customer_points_rules` | project_id | VARCHAR(64) | `direct`，综合体/项目级隔离 |
| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（**敏感字段 at-rest 加密为后续项**，见 §6 ISO-GAP） |

> 注：迁移用 retrofit 方式事后加列，类型为 `VARCHAR`；早期 dev-design 文档把 merchant_id 写成 `uuid`，以本规格与迁移现实为准（待回头修正 design 文档，P2）。完整逐表 scope 映射见 `domain-ownership-scope-audit-v0.1.csv`。

<!-- DATA_ISOLATION_SPEC_AUTO_START -->
### 3A. 自动迁移隔离基线（由 `npm run docs:data-isolation-spec` 生成）
> 事实源：`backend/src/main/resources/db/migration`。当前解析到最新迁移 **V396**，发现 **170** 张含 scope 字段的表。不要手工修改本区块；新增迁移后运行 `npm run docs:data-isolation-spec`，提交前运行 `npm run verify:data-isolation-spec`。
| 表 | scope 字段 | scope 类型 | 首次出现迁移 | 证据文件 |
|---|---|---|---|---|
| `access_grant_requests` | `merchant_id` + `project_id` | direct | V170 | `V170__access_grant_requests.sql` |
| `access_users` | `merchant_id` + `organization_id` | direct | V003 | `V003__p0_access_permission_authz_slice.sql` |
| `amusement_capacity_config` | `project_id` | direct | V083 | `V083__amusement_three_dim_capacity.sql` |
| `amusement_capacity_snapshot` | `project_id` | direct | V083 | `V083__amusement_three_dim_capacity.sql` |
| `amusement_device_statuses` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_devices` | `merchant_id` + `project_id` | direct | V143 | `V143__amusement_device_safety_cycle.sql` |
| `amusement_group_bookings` | `project_id` | direct | V035 | `V035__p2_amusement_group_private_bookings.sql` |
| `amusement_incidents` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
| `amusement_inspection_tasks` | `merchant_id` + `project_id` | direct | V143 | `V143__amusement_device_safety_cycle.sql` |
| `amusement_maintenance_record` | `merchant_id` + `project_id` | direct | V081 | `V081__amusement_maintenance_record.sql` |
| `amusement_manual_overrides` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_over_capacity_policy` | `project_id` | direct | V104 | `V104__amusement_incident_type_over_capacity.sql` |
| `amusement_project_entitlements` | `project_id` | direct | V082 | `V082__amusement_project_entitlement_binding.sql` |
| `amusement_project_rules` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_projects` | `merchant_id` + `space_id` | direct | V049, V084 | `V049__amusement_security_fixes.sql`<br>`V084__object_space_mapping.sql` |
| `amusement_queue_entries` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_reservations` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_safety_inspections` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_safety_notices` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
| `amusement_safety_templates` | `merchant_id` | direct | V049 | `V049__amusement_security_fixes.sql` |
| `amusement_sessions` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_verifications` | `project_id` | direct | V014 | `V014__p0_amusement_safety_session_verification.sql` |
| `amusement_waiver_acknowledgements` | `project_id` | direct | V024 | `V024__p1_amusement_safety_waiver_incident.sql` |
| `analytics_dashboard_snapshots` | `project_id` | direct | V017 | `V017__p0_analytics_ai_quality_risk_dashboard.sql` |
| `analytics_merchant_health_scores` | `merchant_id` | direct | V017 | `V017__p0_analytics_ai_quality_risk_dashboard.sql` |
| `analytics_merchant_summaries` | `merchant_id` | direct | V017 | `V017__p0_analytics_ai_quality_risk_dashboard.sql` |
| `ar_ap_items` | `merchant_id` + `project_id` | direct | V044, V243 | `V044__finance_security_isolation.sql`<br>`V243__ar_ap_project_isolation.sql` |
| `audit_events` | `merchant_id` | direct | V045 | `V045__evidence_security_isolation.sql` |
| `bar_bill_settlements` | `merchant_id` | direct | V062 | `V062__bar_bill_settlement_channel.sql` |
| `bar_bills` | `merchant_id` | direct | V047 | `V047__bar_security_merchant_isolation.sql` |
| `bar_combos` | `merchant_id` | direct | V257 | `V257__bar_combo.sql` |
| `bar_commission_rate_configs` | `merchant_id` + `project_id` | direct | V340 | `V340__commission_and_points_rate_configs.sql` |
| `bar_deposit_rules` | `merchant_id` | direct | V073 | `V073__bar_reservation_deposit.sql` |
| `bar_gift_redemptions` | `merchant_id` | direct | V072 | `V072__bar_gift_redemption.sql` |
| `bar_order_item_voids` | `merchant_id` | direct | V091 | `V091__bar_pos_depth.sql` |
| `bar_orders` | `merchant_id` | direct | V047 | `V047__bar_security_merchant_isolation.sql` |
| `bar_product_categories` | `merchant_id` | direct | V256 | `V256__bar_pos_catalog_category_image.sql` |
| `bar_production_tickets` | `merchant_id` | direct | V047 | `V047__bar_security_merchant_isolation.sql` |
| `bar_products` | `merchant_id` | direct | V047 | `V047__bar_security_merchant_isolation.sql` |
| `bar_reservations` | `merchant_id` | direct | V047 | `V047__bar_security_merchant_isolation.sql` |
| `bar_shifts` | `merchant_id` + `project_id` | direct | V095 | `V095__bar_shift_handover.sql` |
| `bar_table_session_adjustments` | `merchant_id` | direct | V019 | `V019__p1_bar_table_session_adjustments.sql` |
| `bar_table_sessions` | `merchant_id` | direct | V019 | `V019__p1_bar_table_session_adjustments.sql` |
| `bar_tables` | `merchant_id` + `space_id` | direct | V047, V084 | `V047__bar_security_merchant_isolation.sql`<br>`V084__object_space_mapping.sql` |
| `business_entity_bindings` | `merchant_id` | direct | V010 | `V010__p0_foundation_residual_platform_terminal_evidence.sql` |
| `business_process_instances` | `merchant_id` + `project_id` + `venue_id` | direct | V059 | `V059__operation_traceability_three_layer.sql` |
| `business_review_locks` | `project_id` | direct | V216 | `V216__business_review_locks.sql` |
| `channel_fee_configs` | `project_id` | direct | V327 | `V327__settlement_channel_fee_configs.sql` |
| `complex_operating_params` | `project_id` | direct | V160 | `V160__complex_operating_params.sql` |
| `compliance_expiry_items` | `merchant_id` | direct | V028 | `V028__p1_contract_compliance_expiry.sql` |
| `compliance_reminders` | `merchant_id` | direct | V051 | `V051__compliance_security_fixes.sql` |
| `comprehensive_service_fee_configs` | `merchant_id` + `project_id` | direct | V268 | `V268__settlement_comprehensive_service_fee_and_combo_discount.sql` |
| `contract_approval_flows` | `project_id` | direct | V162 | `V162__contract_lifecycle.sql` |
| `contract_changes` | `project_id` | direct | V162 | `V162__contract_lifecycle.sql` |
| `contract_key_dates` | `merchant_id` | direct | V028 | `V028__p1_contract_compliance_expiry.sql` |
| `contract_rent_bills` | `merchant_id` + `project_id` | direct | V162 | `V162__contract_lifecycle.sql` |
| `contract_settlements` | `project_id` | direct | V162 | `V162__contract_lifecycle.sql` |
| `contract_spaces` | `project_id` + `space_id` | direct | V080, V162 | `V080__contract_scope_and_spaces.sql`<br>`V162__contract_lifecycle.sql` |
| `contracts` | `merchant_id` + `project_id` | direct | V010, V162 | `V010__p0_foundation_residual_platform_terminal_evidence.sql`<br>`V162__contract_lifecycle.sql` |
| `customer_birthday_rules` | `project_id` | direct | V089 | `V089__member_loyalty_level_points.sql` |
| `customer_check_in_tiers` | `project_id` | direct | V089 | `V089__member_loyalty_level_points.sql` |
| `customer_member_levels` | `project_id` | direct | V089 | `V089__member_loyalty_level_points.sql` |
| `customer_points_rules` | `project_id` | direct | V060 | `V060__customer_points_rules_and_gift_convergence.sql` |
| `customer_redemption_catalog_items` | `project_id` | direct | V060 | `V060__customer_points_rules_and_gift_convergence.sql` |
| `customer_stored_value_accounts` | `merchant_id` + `project_id` | direct | V090, V175 | `V090__member_stored_value_paid_membership_transfer.sql`<br>`V175__stored_value_merchant_scoped_cards.sql` |
| `deposit_ledger` | `merchant_id` | direct | V110 | `V110__complex_billing.sql` |
| `dividend_plans` | `merchant_id` | direct | V299 | `V299__dividend_engine.sql` |
| `dividend_reserve_releases` | `merchant_id` | direct | V304 | `V304__dividend_reserve_release.sql` |
| `dividend_statement_lines` | `merchant_id` | direct | V299 | `V299__dividend_engine.sql` |
| `dividend_statements` | `merchant_id` | direct | V299 | `V299__dividend_engine.sql` |
| `energy_meters` | `merchant_id` + `project_id` + `space_id` | direct | V168 | `V168__energy_metering.sql` |
| `entitlement_sessions` | `merchant_id` + `project_id` | direct | V129 | `V129__entitlement_sessions.sql` |
| `entitlement_templates` | `merchant_id` + `venue_id` | direct | V075 | `V075__entitlement_merchant_and_ticket_validity.sql` |
| `evidence_attachments` | `merchant_id` | direct | V045 | `V045__evidence_security_isolation.sql` |
| `evidence_events` | `merchant_id` | direct | V045 | `V045__evidence_security_isolation.sql` |
| `evidence_reviews` | `merchant_id` | direct | V045 | `V045__evidence_security_isolation.sql` |
| `external_partner_orgs` | `project_id` | direct | V183 | `V183__external_partner_org_self_service.sql` |
| `facility_equipment` | `merchant_id` + `project_id` + `space_id` | direct | V165 | `V165__facility_equipment.sql` |
| `facility_inspection_tasks` | `project_id` + `space_id` | direct | V167 | `V167__facility_inspection.sql` |
| `facility_inspection_templates` | `project_id` | direct | V167 | `V167__facility_inspection.sql` |
| `facility_work_orders` | `merchant_id` + `project_id` + `space_id` | direct | V166 | `V166__facility_work_orders.sql` |
| `giveaway_cost_configs` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `high_value_thresholds` | `merchant_id` + `project_id` | direct | V350 | `V350__high_value_threshold_unify.sql` |
| `integration_channel_store_map` | `merchant_id` | direct | V334 | `V334__integration_order_sync_merchant_attribution.sql` |
| `integration_order_syncs` | `merchant_id` | direct | V334 | `V334__integration_order_sync_merchant_attribution.sql` |
| `internal_price_catalog` | `merchant_id` | direct | V379 | `V379__internal_price_catalog.sql` |
| `inventory_high_value_reviews` | `merchant_id` | direct | V237 | `V237__inventory_isolation_fixes.sql` |
| `inventory_item_cost_state` | `merchant_id` | direct | V296 | `V296__inventory_cost_ledger.sql` |
| `inventory_items` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_purchase_requests` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_risk_alerts` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_stock_adjustments` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_stock_balances` | `merchant_id` + `project_id` | direct | V046, V237 | `V046__inventory_security_fixes.sql`<br>`V237__inventory_isolation_fixes.sql` |
| `inventory_stock_batches` | `merchant_id` + `project_id` | direct | V046, V244 | `V046__inventory_security_fixes.sql`<br>`V244__inventory_movement_batch_project_isolation.sql` |
| `inventory_stock_movements` | `merchant_id` + `project_id` | direct | V046, V244 | `V046__inventory_security_fixes.sql`<br>`V244__inventory_movement_batch_project_isolation.sql` |
| `inventory_supplier_reconciliations` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_suppliers` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `inventory_trace_codes` | `merchant_id` | direct | V128 | `V128__inventory_trace_codes.sql` |
| `inventory_trace_handovers` | `merchant_id` | direct | V139 | `V139__inventory_trace_handover_merchant_id.sql` |
| `invoice_subject_rules` | `project_id` | direct | V362 | `V362__settlement_invoice_list.sql` |
| `issued_entitlements` | `merchant_id` + `venue_id` | direct | V006 | `V006__p0_ticketing_verification.sql` |
| `leasing_brands` | `project_id` | direct | V161 | `V161__leasing_acquisition.sql` |
| `leasing_follow_ups` | `project_id` | direct | V161 | `V161__leasing_acquisition.sql` |
| `leasing_intents` | `project_id` | direct | V161 | `V161__leasing_acquisition.sql` |
| `leasing_leads` | `project_id` | direct | V161 | `V161__leasing_acquisition.sql` |
| `mall_floor_plans` | `merchant_id` + `project_id` | direct | V023, V052 | `V023__p1_floor_plan_polygons.sql`<br>`V052__floorplan_merchant_isolation.sql` |
| `merchant_bill_disputes` | `merchant_id` | direct | V026 | `V026__p1_merchant_billing_portal.sql` |
| `merchant_bill_dunning_records` | `merchant_id` | direct | V026 | `V026__p1_merchant_billing_portal.sql` |
| `merchant_bills` | `merchant_id` | direct | V026 | `V026__p1_merchant_billing_portal.sql` |
| `merchant_expense_entries` | `merchant_id` | direct | V297 | `V297__merchant_expense_ledger.sql` |
| `merchant_fee_notification_recipient` | `merchant_id` | direct | V182 | `V182__merchant_fee_notification_recipient.sql` |
| `merchant_revenue_attribution_rules` | `merchant_id` | direct | V221 | `V221__merchant_revenue_attribution_rules.sql` |
| `merchant_service_work_orders` | `merchant_id` + `space_id` | direct | V022 | `V022__p1_merchant_service_work_orders.sql` |
| `pass_gift_cost_configs` | `project_id` | direct | V373 | `V373__pass_gift_cost_configs.sql` |
| `pass_gift_cost_variance_ledger` | `project_id` | direct | V377 | `V377__pass_gift_variance_ledger.sql` |
| `platform_retention_rules` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `points_accrual_rate_configs` | `merchant_id` + `project_id` | direct | V340 | `V340__commission_and_points_rate_configs.sql` |
| `position_role_templates` | `organization_id` | direct | V169 | `V169__access_role_assignment_closure.sql` |
| `projects` | `merchant_id` | direct | V005 | `V005__p0_platform_master_data.sql` |
| `property_fee_bills` | `merchant_id` + `project_id` | direct | V150 | `V150__property_fee_fixed_billing.sql` |
| `property_fee_rates` | `project_id` | direct | V150 | `V150__property_fee_fixed_billing.sql` |
| `refund_authority_limits` | `project_id` | direct | V156 | `V156__refund_tiered_authorization.sql` |
| `refund_auto_rules` | `project_id` | direct | V156 | `V156__refund_tiered_authorization.sql` |
| `revenue_stream_attribution` | `project_id` | direct | V319 | `V319__revenue_stream_attribution.sql` |
| `risk_alerts` | `merchant_id` + `project_id` | direct | V103 | `V103__unified_risk_alerts.sql` |
| `roles` | `organization_id` | direct | V048 | `V048__access_role_organization_isolation.sql` |
| `saved_bottles` | `merchant_id` | direct | V046 | `V046__inventory_security_fixes.sql` |
| `service_orders` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `service_responsibility_records` | `merchant_id` | direct | V059 | `V059__operation_traceability_three_layer.sql` |
| `settlement_bills` | `project_id` | direct | V224 | `V224__settlement_bills_batch2.sql` |
| `settlement_invoice_items` | `project_id` | direct | V362 | `V362__settlement_invoice_list.sql` |
| `settlement_items` | `merchant_id` | direct | V224 | `V224__settlement_bills_batch2.sql` |
| `settlement_tax_configs` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `settlement_transactions` | `merchant_id` + `project_id` | direct | V222 | `V222__settlement_transactions_batch1.sql` |
| `site_entry_reports` | `project_id` + `space_id` | direct | V179 | `V179__site_entry_reports.sql` |
| `space_expiry_color_rules` | `project_id` | direct | V162 | `V162__contract_lifecycle.sql` |
| `spaces` | `merchant_id` + `project_id` | direct | V005 | `V005__p0_platform_master_data.sql` |
| `staff_attendance_records` | `project_id` + `space_id` | direct | V210, V214 | `V210__staff_shift_attendance.sql`<br>`V214__tenant_scope_staff_service_tables.sql` |
| `staff_base_salaries` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `staff_commission_ledger` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `staff_leave_requests` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `staff_level_assignments` | `merchant_id` + `organization_id` | direct | V055 | `V055__access_staff_levels_and_salary.sql` |
| `staff_payroll_adjustments` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `staff_payroll_payouts` | `project_id` | direct | V238 | `V238__payroll_statement_paid_state.sql` |
| `staff_payroll_statements` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `staff_profiles` | `merchant_id` + `project_id` | direct | V086 | `V086__staff_tables.sql` |
| `staff_shift_schedules` | `project_id` | direct | V214 | `V214__tenant_scope_staff_service_tables.sql` |
| `stocktake` | `merchant_id` | direct | V108 | `V108__inventory_variance_to_ap.sql` |
| `stored_value_merchant_refund_rules` | `merchant_id` | direct | V176 | `V176__stored_value_merchant_refund_rules.sql` |
| `stored_value_topup_intents` | `merchant_id` + `project_id` | direct | V284 | `V284__stored_value_topup_intents.sql` |
| `supplier_qualifications` | `merchant_id` | direct | V112 | `V112__supplier_qualification_settlement.sql` |
| `supplier_settlement_methods` | `merchant_id` | direct | V112 | `V112__supplier_qualification_settlement.sql` |
| `supply_contracts` | `merchant_id` + `project_id` | direct | V085 | `V085__supply_chain_t6.sql` |
| `terminal_devices` | `merchant_id` + `venue_id` | direct | V007 | `V007__p0_terminal_evidence.sql` |
| `ticket_combo_discount_configs` | `project_id` | direct | V268 | `V268__settlement_comprehensive_service_fee_and_combo_discount.sql` |
| `ticket_distribution_configs` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `ticket_distribution_merchant_rates` | `merchant_id` + `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `ticket_distribution_rank_tiers` | `project_id` | direct | V223 | `V223__settlement_distribution_configs_batch2.sql` |
| `ticket_orders` | `merchant_id` | direct | V135 | `V135__ticket_orders_merchant_id.sql` |
| `ticket_refunds` | `merchant_id` | direct | V220 | `V220__ticket_refunds_merchant_id.sql` |
| `ticket_verifications` | `merchant_id` | direct | V066 | `V066__ticket_anticounterfeit_window_nonce.sql` |
| `turnover_rent_rules` | `merchant_id` | direct | V110 | `V110__complex_billing.sql` |
| `user_position_assignments` | `merchant_id` + `organization_id` | direct | V169 | `V169__access_role_assignment_closure.sql` |
| `venue_business_hours` | `venue_id` | inherited-or-location-scope | V226 | `V226__entitlement_session_kind_batch3.sql` |
| `venue_sessions` | `venue_id` | inherited-or-location-scope | V226 | `V226__entitlement_session_kind_batch3.sql` |
| `voucher_definitions` | `merchant_id` | direct | V301 | `V301__unified_voucher_ledger.sql` |
| `voucher_redemption_records` | `merchant_id` | direct | V301 | `V301__unified_voucher_ledger.sql` |
| `warehouses` | `merchant_id` + `project_id` | direct | V085 | `V085__supply_chain_t6.sql` |
| `wecom_conversations` | `project_id` | direct | V087 | `V087__wecom_compliance.sql` |
| `wecom_risk_hits` | `project_id` | direct | V106 | `V106__wecom_risk_hits_and_rules.sql` |

> 自动基线说明：本区块只证明“迁移中出现了 scope 字段”，不替代 `verify:tenant-isolation` 对 Repository scope 过滤的机审，也不替代越权 E2E/单测。
<!-- DATA_ISOLATION_SPEC_AUTO_END -->

## 4. 运行时越权拦截与验收标准（Given/When/Then）

每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` 绑定）：

- **AC-ISO-1 读隔离**：
  - Given 商户 A 的员工 token，存在商户 B 的数据；
  - When 调用任一列表/详情接口；
  - Then 只返回 A 的数据；B 的数据不出现（按 direct 列过滤或 inherited join 过滤）。
- **AC-ISO-2 写隔离 / 注入不可被参数覆盖**：
  - Given 商户 A token，请求体里**伪造** `merchant_id=B`；
  - When 提交写操作；
  - Then 落库的 scope 必须是 token 的 A，请求体中的 B 被忽略（不得越权写入 B）。
- **AC-ISO-3 平台级**：Given 平台管理员（scope 空）→ 可见全量。
- **AC-ISO-4 旧行可见性**：明确 `merchant_id IS NULL` 旧行对商户角色是否可见（默认：不可见，除非显式回填）。

逐表用例应覆盖 §3/§3A 全部应隔离表；`verify:tenant-isolation` 已作为契约级守卫纳入交付闸口。

## 5. 旧行回填策略（待执行）

- 存量 `merchant_id IS NULL` 行需按业务归属回填正确 scope，否则商户角色看不到历史数据。
- 回填后将相关部分索引/约束按需 VALIDATE（含 V046 的 `chk_stock_adjustment_type` NOT VALID → VALIDATE）。
- 回填动作应有迁移或一次性脚本 + 审计留痕；本规格不指定具体数值，由数据负责人按真实归属执行。

## 6. 仍缺口（doc/工程，已登记为本轮 P0/P1）

| 编号 | 缺口 | 级别 |
|---|---|---|
| ISO-GAP-01 | **已关闭（2026-06-21）**：安全基线生成器已自动纳管 `SEC-P0-TENANT-ISOLATION-001`，`findings-register-v1.md` / `formal-security-baseline-v1.md` 均由 `docs:security-baseline` 生成；`verify:tenant-isolation` 已纳入交付闸口并通过 | Closed |
| ISO-GAP-02 | **已关闭（2026-06-21）**：`verify:tenant-isolation` 自动闸口已落地并接入 `verify:delivery-gates`，可检查 repository scope 注入与新增表登记 | Closed |
| ISO-GAP-03 | **已关闭（V065）**：`V065__amusement_merchant_isolation_indexes.sql` 已给 amusement 4 表 merchant_id 建部分索引（原「无索引」P1 为过期标注，2026-07-02 回写并经库内索引核实） | Closed |
| ISO-GAP-04 | V044–V061 缺 TDD 风格实现/回归 plan（对齐 `superpowers/plans/2026-06-15-security-audit-fixes.md` 的 V038/V039 记录）→ 详见本节末「ISO-GAP-04 TDD 回归 plan 待补」 | P1 |
| ISO-GAP-05 | dev-design（bar/amusement/foundation）把 merchant_id 写成 `uuid`，与迁移 `VARCHAR` retrofit 不符 | P2 |
| ISO-GAP-06 | `saved_bottles.customer_phone` 等 V061 新增敏感字段 **at-rest 加密未实现**（明文存储）→ 应补加密/脱敏方案 | P1 |

> **缺口登记状态（截至 2026-06-18，迁移基线 V061）**：V053 / V054 / V055（staff_level_assignments + staff_levels）/ V057（supervisor_review_decisions）/ V059（business_process_instances + business_process_step_records + service_responsibility_records）/ V060（customer_points_rules）/ V061（saved_bottles 补字段）均已在本规格 §3 逐表清单登记，scope_type/scope_source 已标注。后续新增带租户数据的表须继续按 §8 双向绑定登记。

### ISO-GAP-04 TDD 回归 plan 待补（F7）

V044–V061 各隔离迁移目前**缺统一的 TDD 风格回归 plan / 用例集**，隔离行为只散落在域内单测与人工口径中，无单一权威回归清单。回归 plan 落地前，应至少覆盖以下表（按 AC-ISO-1 读隔离 / AC-ISO-2 写注入不可被参数覆盖 / AC-ISO-3 平台级 / AC-ISO-4 旧行可见性 四条）：

- V044：`ar_ap_items`（direct merchant_id）+ finance_daily_closes scope 唯一约束
- V046：inventory 10 表 + `saved_bottles`（direct merchant_id）
- V047：bar_tables / bar_reservations / bar_products / bar_orders / bar_production_tickets / bar_bills（direct merchant_id）
- V048：`roles`（direct organization_id，NULL=平台全局）
- V049：amusement 4 表（direct merchant_id，部分索引已补 V065）
- V051：`compliance_reminders`（direct merchant_id）
- V052：`mall_floor_plans`（direct merchant_id）
- V055：`staff_level_assignments`（direct org+merchant）；`staff_levels` 平台全局参考无 scope（只测全量可见）
- V057：`supervisor_review_decisions`（无 scope 列 → 测审批留痕字段不被当作隔离键；隔离裁决待治理）
- V059：`business_process_instances`（direct merchant_id+project_id）、`business_process_step_records`（inherited via process_instance_id）、`service_responsibility_records`（direct merchant_id）
- V060：`customer_points_rules`（direct project_id，综合体级）
- V061：`saved_bottles`（direct merchant_id；新增 customer_phone 等敏感字段加密为 ISO-GAP-06）

该 plan 已对齐 `verify:tenant-isolation`（ISO-GAP-02）契约闸口，作为机审守卫与单测的回归底座。

## 6A. 租户隔离机审闸口与越权台账（verify:tenant-isolation）

> 修正(2026-06-21)：`verify:tenant-isolation` 自动闸口与 `SEC-P0-TENANT-ISOLATION-001` finding 已由安全基线生成器纳管，闭合“机审可发现未隔离查询、越权风险进入台账、修复后回归”的治理链路。

### 6A.1 CI 机审闸口（`verify:tenant-isolation`）

闸口职责：扫描含 `merchant_id` / `project_id`（及 `organization_id` 等 scope 列，见 §1 语义）的表与查询，校验是否**强制注入 scope**，缺失即失败。

- **扫描对象：**
  - **表清单**：以 `domain-ownership-scope-audit-v0.1.csv` 为权威表清单（§8 双向绑定），逐表确认 scope_type/scope_source 已登记；新增带租户数据的表未登记 → 失败。
  - **查询/Repository**：扫描访问上述表的查询（JPA/Repository/原生 SQL），校验 scope 谓词来自**安全 token 注入**（§2 权威约定：scope 绝不取请求参数）；存在未注入 scope 的读/写查询 → 失败。
- **失败口径（fail-closed）：** 任一"应隔离表的查询缺 scope 注入""scope 来源为请求参数""新增表未在 CSV/§3 登记"→ 闸口判失败，阻断合入。
- **白名单例外：** 平台全局参考表（如 `staff_levels`、`roles` NULL=平台全局）须显式登记为"无 scope/平台全局"，方可豁免行级 scope 注入校验，避免误报。
- **读路径谓词写法（2026-07-28 P0-D 教训）：** 带 scope 列的表，读路径一律写 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`，**禁止**用 `COALESCE(<scope_col>, '__DEFAULT__') IN (...)` 哨兵比对。后者对常规数据语义等价，但 ① 机审识别不出 scoped 通道（会误报 F1）；② 依赖写面 rejectSentinel 保证「没有真 id 叫 `__DEFAULT__`」——一旦撞名，该行对**所有**调用方可见 = 真跨租户读（唯一索引只挡顶层默认槽，商户维不挡）。证据见 `SEC-P2-TENANT-ISOLATION-P0D-CONFIG-SCOPE-001`。
- **与现有机审关系：** 现有机审基线只统计 OpenAPI auth context（验不到 DB 行级隔离），本闸口补 DB/查询级行级隔离校验，二者互补，不重复。

### 6A.2 越权 finding 台账（证据留痕）

- **台账落点：** 权威安全 finding 台账（`security-scans/findings-register-v1.md` / `formal-security-baseline-v1.md`）已由生成器纳管 `SEC-P0-TENANT-ISOLATION-001`（对齐 ISO-GAP-01），后续同类条目不得仅手工补登。
- **每条 finding 记录证据：** 构造的越权请求（跨商户/跨项目读或写）、token 的真实 scope、请求体中尝试越权的 scope、实际结果（被拒/被忽略写入 token scope）、对应表与用例编号（AC-ISO-1/2/3/4，见 §4、§6 ISO-GAP-04 表清单）。
- **机审发现项入台账：** `verify:tenant-isolation` 扫出的"未隔离查询/未登记表"也作为 finding 入台账并跟踪闭环，形成"机审发现 → 台账登记 → 修复 → 回归"链路。

### 6A.3 验收

- **越权被拒并留痕：** 构造跨商户、跨项目越权请求（读与写），读被隔离（看不到他人数据）、写被强制落 token 的 scope（请求体越权 scope 被忽略，见 §4 Given/When/Then），且每条用例在越权台账留下证据条目。
- **机审可发现未隔离查询：** 故意引入一条"应隔离表缺 scope 注入"或"新增表未登记 CSV/§3"的变更，`verify:tenant-isolation` 必须判失败并阻断；修复后通过。
- **回归底座对齐：** 验收用例集对齐 §6 ISO-GAP-04 表清单（V044–V061）与 AC-ISO-1/2/3/4 四条，作为机审与单测共用回归底座。

> **已收口：** `verify:tenant-isolation` 已纳入 `verify:delivery-gates`，与 OpenAPI、矩阵、迁移、安全基线闸口共同作为交付阻断检查。

## 7. V050 主管复核持久化数据模型（此前仅在 .sql，本规格补字段级文档）

表 `supervisor_review_decisions`（V050 新建）：

| 列 | 类型 | 说明 |
|---|---|---|
| review_id | VARCHAR(64) PK | 复核记录主键 |
| policy_code | VARCHAR(100) NOT NULL | 命中的复核策略码 |
| action_type / object_type | VARCHAR(100) NOT NULL | 触发动作类型 / 对象类型 |
| object_id | VARCHAR(256) NOT NULL | 对象 ID |
| blocked | BOOLEAN NOT NULL | 是否默认阻断 |
| review_status | VARCHAR(50) NOT NULL | pending_review / approved / … |
| required_level | INT NOT NULL | 要求主管级别 |
| reviewer_level | INT | 实际审批人级别 |
| decision | VARCHAR(50) | 审批决定 |
| reason | TEXT | 审批意见 |
| evidence_attachment_ids | JSONB NOT NULL DEFAULT '[]' | 证据附件 |
| evaluated_at | TIMESTAMPTZ NOT NULL | 评估时间 |

索引：`idx_supervisor_review_decisions_pending` 部分索引 `WHERE review_status='pending_review'`。
业务/权限/事件口径见 `platform-governance-product-spec-v0.2.md`、OpenAPI `governance.supervisor_review.view/manage/approve`。
**隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131` 标注 "no direct scope column detected"）——需决策复核记录是否需按 organization/merchant 隔离（登记为 ISO-GAP，待治理负责人裁决）。

## 8. 字段级验收 = 与盘点 CSV 双向绑定

本规格 §3 表 ↔ `domain-ownership-scope-audit-v0.1.csv` 逐行对应。任何新增带租户数据的表，必须：(1) 在 CSV 增行并标 scope_type/scope_source；(2) 在本规格 §3 增行；(3) 补 AC-ISO-1/2 用例。CSV 不再是 orphan，由本规格正式承接解释。

## 9. append-only 防篡改与受限运行时角色（V140）

审计/证据/流水/留痕表的不可变性由 DB 层硬强制，不依赖应用自觉：

- **触发器（全环境生效，对 superuser/owner 亦触发）**：通用阻断函数 `complex_block_mutation()`，UPDATE/DELETE 命中即抛 `COMPLEX_APPEND_ONLY_VIOLATION`。
  - **不可变表（UPDATE+DELETE 双锁，15 张）**：`audit_events`、`evidence_events`、`customer_stored_value_transactions`、`deposit_ledger`、`risk_alert_actions`、`ar_ap_payments`、`integration_robot_task_events`、`terminal_device_status_events`、`inventory_purchase_request_events`、`integration_video_access_audits`、`wecom_access_audits`、`cam_trueup_lines`、`cam_trueup_runs`、`integration_read_ledger`、`permission_template_usage_logs`。
  - **永久留痕表（仅 DELETE 锁，保留幂等 UPDATE，15 张）**：`evidence_reviews`/`evidence_attachments`、`ar_ap_item_sources`、`bar_production_ticket_events`、`open_api_events`、`inventory_stock_movements`、`finance_reconciliation_differences`/`batches`、`ticket_payment_intents`/`finance_payment_intents`、`inventory_trace_handovers`/`steps`/`exceptions`、`ticket_refund_lines`、`customer_points_ledger`。
- **CASCADE 中止**：删父业务行的级联删除触达受保护子表即触发 trigger 整事务回滚——等价 `ON DELETE CASCADE → RESTRICT` 的资金/审计保护，且比 FK 改造更鲁棒（未改 FK）。
- **受限运行时角色 `complex_app`（生产）**：迁移 V140 以 owner 身份创建受限 role，REVOKE 不可变表 UPDATE/DELETE、永久留痕表 DELETE；运行时连接（`application-prod.yml` 的 `spring.datasource`）改用 `complex_app`，Flyway 仍以 owner 跑 DDL。`complex_app` 无 DDL 权限，无法 `DROP TRIGGER` 或 `SET session_replication_role` 绕过。未配置 app 专用 env 时回退 owner 凭证（向后兼容）。
- **最小权限基线**：上述受限 role + append-only 触发器纳入安全基线检查项（`formal-security-baseline`）。

## 10. 特殊人员组织边界（外包 / 检修单位 / 审计人员）（2026-06-26 审计第二轮补，裁决#8）

> 业务 HTML 对一般员工、商户、客人的数据边界已清晰；本节补此前偏弱的三类特殊主体最小权限边界，使「谁能看/改什么范围」对外包、检修单位、审计人员也可追溯。口径向业务 HTML（权限和职级 / 商户侧 / 留痕复核）对齐；正式权限点以 `p0-operation-permission-matrix-v0.1.md` 为准。

| 主体 | 身份归属 | 可见 / 可操作范围 | 明确不可 | 落地承接 |
|---|---|---|---|---|
| **外包 / 检修单位** | 独立角色账号（账号类型 `outsourced_vendor`，区别于仅作台账的供货供应商记录），不进员工组织树 | 仅派给本单位/本人的工单/巡检/进场报备上下文（`data_scope=assignee`，角色 OUTSOURCED_FACILITY/OUTSOURCED_SECURITY_CLEANING/OUTSOURCED_CONSTRUCTION，细权限均为 `*_assigned`）；按授权范围只读或限定写 | 平台全局、其他商户/项目数据、财务与客人敏感信息、跨工单批量数据 | 已落地（V178 外包账号+facility assignee 强制、V179 进场报备）；检修记录须主管确认才生效，禁止巡检员/检修方单人闭环 |
| **审计人员** | 平台员工，单独审计角色 | 跨域**只读**留痕/审计/复核记录（按授权综合体范围）；查看企微原文/视频证据/财务敏感信息属敏感操作 | 任何业务写操作、绕过 append-only 改记录、无审批查看敏感原文 | 已落地（V180 两级只读角色：内审 complex 全量只读 / 外审 project 限期只读，0 写权）；查看敏感原文须最小授权+审批并写访问审计（留痕复核节）；审计/证据表 append-only（见 §9） |
| **商户外包员工** | 归属某商户，受该商户授权 | 仅该商户授权业务范围（同商户侧边界） | 跨商户、平台全局（同 §1 商户隔离） | 复用商户侧 scope（§1/§2 token 注入 scope，绝不取请求参数） |

- **统一原则**：三类主体一律走 §2「scope 只来自安全 token」，不得以请求参数放大范围；其访问（含被拒）按 HTML 留痕复核要求写审计（外包检修动作、审计人员敏感查看）。
- **落地现状（2026-06-30 更新，纠正旧"待落地核对"措辞）**：外包与审计已落地——外包账号类型 `outsourced_vendor` + 三角色（OUTSOURCED_FACILITY / OUTSOURCED_SECURITY_CLEANING / OUTSOURCED_CONSTRUCTION）assignee 级数据范围强制（list 按 assignee 过滤、写操作 FORBIDDEN、跨商户/平台拒绝），审计两级只读角色（V180）；权限点已在 `full-admin-page-permission-field-matrix`（外包工作台 ADM-VND 页），详见治理 PRD §4，verify:full-admin-matrix 已绿。外包账号**独立于供货供应商台账**（供货供应商只作采购/对账对象、零登录账号）。
- **自助下属层（已全部落地，2026-07-02 回写）**：外部协作机构自助开通/停用下属账号、平台设下属账号上限、账号有效期随合同周期到期自动停用、下属继承机构范围、机构查看下属工作完成情况（`PartnerSubordinateCompletionService` + subordinates-completion 端点 + `ADM-VND-004` 页）均已落地；原「尚未实现」为过期标注。需求口径见治理 PRD §4。
