package com.complex.platform.ticketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTicketingRepository implements TicketingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTicketingRowMapper rowMapper;
    private final JdbcTicketingParameterSource parameterSource;

    JdbcTicketingRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        var jdbcObjectMapper = objectMapper.copy().findAndRegisterModules();
        this.rowMapper = new JdbcTicketingRowMapper(jdbcObjectMapper);
        this.parameterSource = new JdbcTicketingParameterSource(jdbcObjectMapper);
    }

    @Override
    public TicketTypeRecord saveTicketType(TicketTypeRecord ticketType) {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (
                    id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                    holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                    stock_quantity, sold_quantity, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_name, :base_calendar_price_mode, :weekday_price_cent, :weekend_price_cent,
                    :holiday_price_cent, :stock_required, :status, :validity_days, :refresh_interval_seconds,
                    :stock_quantity, :sold_quantity, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_name = EXCLUDED.ticket_name,
                    base_calendar_price_mode = EXCLUDED.base_calendar_price_mode,
                    weekday_price_cent = EXCLUDED.weekday_price_cent,
                    weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent,
                    stock_required = EXCLUDED.stock_required,
                    status = EXCLUDED.status,
                    validity_days = EXCLUDED.validity_days,
                    refresh_interval_seconds = EXCLUDED.refresh_interval_seconds,
                    stock_quantity = EXCLUDED.stock_quantity,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeParameters(ticketType));
        return ticketType;
    }

    @Override
    public List<TicketTypeRecord> listTicketTypes() {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                       stock_quantity, sold_quantity, created_at, updated_at
                  FROM ticket_types
                 ORDER BY created_at DESC, id
                """, rowMapper::mapTicketType);
    }

    @Override
    public Optional<TicketTypeRecord> findTicketTypeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, validity_days, refresh_interval_seconds,
                       stock_quantity, sold_quantity, created_at, updated_at
                  FROM ticket_types
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTicketType).stream().findFirst();
    }

    @Override
    public boolean atomicDeductTicketStock(String ticketTypeId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE ticket_types
                   SET sold_quantity = sold_quantity + :quantity,
                       updated_at = :now
                 WHERE id = :id
                   AND (stock_quantity IS NULL OR sold_quantity + :quantity <= stock_quantity)
                """, new MapSqlParameterSource()
                .addValue("id", ticketTypeId)
                .addValue("quantity", quantity)
                .addValue("now", java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)));
        return updated > 0;
    }

    @Override
    public void releaseTicketStock(String ticketTypeId, int quantity) {
        jdbcTemplate.update("""
                UPDATE ticket_types
                   SET sold_quantity = GREATEST(0, sold_quantity - :quantity),
                       updated_at = :now
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", ticketTypeId)
                .addValue("quantity", quantity)
                .addValue("now", java.time.Instant.now().atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (
                    id, name, entitlement_type, default_total_times, default_value_cent,
                    status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                    agreement_price_cent, created_at, updated_at
                )
                VALUES (
                    :id, :name, :entitlement_type, :default_total_times, :default_value_cent,
                    :status, :merchant_id, :venue_id, :point_id, :bar_item_ref, :entitlement_kind,
                    :agreement_price_cent, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    entitlement_type = EXCLUDED.entitlement_type,
                    default_total_times = EXCLUDED.default_total_times,
                    default_value_cent = EXCLUDED.default_value_cent,
                    status = EXCLUDED.status,
                    merchant_id = EXCLUDED.merchant_id,
                    venue_id = EXCLUDED.venue_id,
                    point_id = EXCLUDED.point_id,
                    bar_item_ref = EXCLUDED.bar_item_ref,
                    entitlement_kind = EXCLUDED.entitlement_kind,
                    agreement_price_cent = EXCLUDED.agreement_price_cent,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.entitlementTemplateParameters(template));
        return template;
    }

    @Override
    public List<EntitlementTemplateRecord> listEntitlementTemplates() {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                       agreement_price_cent, created_at, updated_at
                  FROM entitlement_templates
                 ORDER BY created_at DESC, id
                """, rowMapper::mapEntitlementTemplate);
    }

    @Override
    public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, merchant_id, venue_id, point_id, bar_item_ref, entitlement_kind,
                       agreement_price_cent, created_at, updated_at
                  FROM entitlement_templates
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapEntitlementTemplate).stream().findFirst();
    }

    @Override
    public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO ticket_type_entitlements (
                    id, ticket_type_id, entitlement_template_id, quantity, total_times,
                    value_amount_cent, effective_rule, num_people, component_price_cent, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :entitlement_template_id, :quantity, :total_times,
                    :value_amount_cent, :effective_rule, :num_people, :component_price_cent, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    entitlement_template_id = EXCLUDED.entitlement_template_id,
                    quantity = EXCLUDED.quantity,
                    total_times = EXCLUDED.total_times,
                    value_amount_cent = EXCLUDED.value_amount_cent,
                    effective_rule = EXCLUDED.effective_rule,
                    num_people = EXCLUDED.num_people,
                    component_price_cent = EXCLUDED.component_price_cent,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, entitlement_template_id, quantity, total_times,
                       value_amount_cent, effective_rule, num_people, component_price_cent, created_at, updated_at
                  FROM ticket_type_entitlements
                 WHERE ticket_type_id = :ticket_type_id
                 ORDER BY created_at ASC, id
                """, Map.of("ticket_type_id", ticketTypeId), rowMapper::mapTicketTypeEntitlement);
    }

    @Override
    public IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (
                    id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                    value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                    allocated_revenue_cent, entitlement_kind, agreement_price_cent
                )
                VALUES (
                    :id, :ticket_type_id, :order_id, :entitlement_template_id, :entitlement_code,
                    :entitlement_type, :merchant_id, :venue_id, :point_id, :total_times, :used_times,
                    :value_amount_cent, :status, :valid_from, :valid_to, :created_at, :updated_at, :num_people,
                    :allocated_revenue_cent, :entitlement_kind, :agreement_price_cent
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    order_id = EXCLUDED.order_id,
                    entitlement_template_id = EXCLUDED.entitlement_template_id,
                    entitlement_code = EXCLUDED.entitlement_code,
                    entitlement_type = EXCLUDED.entitlement_type,
                    merchant_id = EXCLUDED.merchant_id,
                    venue_id = EXCLUDED.venue_id,
                    point_id = EXCLUDED.point_id,
                    total_times = EXCLUDED.total_times,
                    used_times = EXCLUDED.used_times,
                    value_amount_cent = EXCLUDED.value_amount_cent,
                    status = EXCLUDED.status,
                    valid_from = EXCLUDED.valid_from,
                    valid_to = EXCLUDED.valid_to,
                    updated_at = EXCLUDED.updated_at,
                    num_people = EXCLUDED.num_people,
                    allocated_revenue_cent = EXCLUDED.allocated_revenue_cent,
                    entitlement_kind = EXCLUDED.entitlement_kind,
                    agreement_price_cent = EXCLUDED.agreement_price_cent
                """, parameterSource.issuedEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                       allocated_revenue_cent, entitlement_kind, agreement_price_cent
                  FROM issued_entitlements
                 WHERE entitlement_code = :entitlement_code
                """, Map.of("entitlement_code", entitlementCode), rowMapper::mapIssuedEntitlement).stream().findFirst();
    }

    @Override
    public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = :used_times,
                       status = :status,
                       updated_at = :updated_at
                 WHERE id = :id
                """, parameterSource.issuedEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public List<OfflineEntitlementResponse> listOfflineEntitlements(String merchantId, String venueId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                       allocated_revenue_cent, entitlement_kind, agreement_price_cent
                  FROM issued_entitlements
                 WHERE status = 'active'
                   AND (merchant_id = :merchant_id OR :merchant_id IS NULL)
                   AND (venue_id = :venue_id OR :venue_id IS NULL)
                 ORDER BY updated_at DESC, id
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("venue_id", venueId)
                .addValue("limit", limit), (resultSet, rowNumber) ->
                TicketEntitlementMapper.toOfflineEntitlementResponse(rowMapper.mapIssuedEntitlement(resultSet, rowNumber)));
    }

    @Override
    public VerificationRecord saveVerification(VerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO ticket_verifications (
                    id, verification_no, entitlement_id, entitlement_code, credential_type,
                    credential_payload, device_id, point_id, status, result_reason, source_type,
                    client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                    verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent
                )
                VALUES (
                    :id, :verification_no, :entitlement_id, :entitlement_code, :credential_type,
                    :credential_payload, :device_id, :point_id, :status, :result_reason, :source_type,
                    :client_task_id, :created_at, :nonce, :staff_id, :merchant_id, :window_valid, :signature_valid,
                    :verified_people, :reverses_verification_id, :reversed_by, :reversed_at, :settlement_price_cent
                )
                """, parameterSource.verificationParameters(verification));
        return verification;
    }

    @Override
    public List<VerificationRecord> listVerifications() {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                       verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent
                  FROM ticket_verifications
                 ORDER BY created_at DESC, id
                """, rowMapper::mapVerification);
    }

    @Override
    public Optional<VerificationRecord> findVerificationById(String id) {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid,
                       verified_people, reverses_verification_id, reversed_by, reversed_at, settlement_price_cent
                  FROM ticket_verifications
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapVerification).stream().findFirst();
    }

    @Override
    public int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt) {
        // F-TXN-04 并发双花修复：状态 CAS——仅当原记录仍为 success 且尚未冲正（reversed_at IS NULL）才翻转。
        // 此前无 WHERE 守卫，service 层 reversedAt==null 读检与本翻转之间存在 TOCTOU：并发两路冲正可同时通过读检、
        // 同时回滚 used_times（多次票回滚两次=权益虚增）。改 CAS 后并发只一路得 1，另一路得 0 → 调用方拒绝。
        return jdbcTemplate.update("""
                UPDATE ticket_verifications
                   SET status = 'reversed',
                       reversed_by = :reversed_by,
                       reversed_at = :reversed_at
                 WHERE id = :id
                   AND status = 'success'
                   AND reversed_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("reversed_by", reversedBy)
                .addValue("reversed_at", reversedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt) {
        // F-TXN-04 并发双拨付修复：退款审批状态 CAS——仅当仍为 requested 才翻转 approved。
        // 此前 approveRefund 读检 status=='requested' 与 saveTicketRefund 间 TOCTOU：并发两路可同时过读检、
        // 各发一次 TicketRefundApprovedEvent（双拨付）。改 CAS 后并发只一路得 1，另一路得 0 → 调用方拒绝。
        return jdbcTemplate.update("""
                UPDATE ticket_refunds
                   SET status = 'approved',
                       approved_by = :approved_by,
                       approved_at = :approved_at,
                       updated_at = :approved_at
                 WHERE id = :id
                   AND status = 'requested'
                """, new MapSqlParameterSource()
                .addValue("id", refundId)
                .addValue("approved_by", approvedBy)
                .addValue("approved_at", approvedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId) {
        return jdbcTemplate.query("""
                SELECT id, device_code, staff_id, client_task_id, task_type, status, reason,
                       verification_id, verification_no, occurred_at, payload::text AS payload, created_at
                  FROM terminal_sync_tasks
                 WHERE device_code = :device_code
                   AND client_task_id = :client_task_id
                """, Map.of(
                "device_code", deviceCode,
                "client_task_id", clientTaskId
        ), rowMapper::mapTerminalSyncTask).stream().findFirst();
    }

    @Override
    public TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task) {
        jdbcTemplate.update("""
                INSERT INTO terminal_sync_tasks (
                    id, device_code, staff_id, client_task_id, task_type, status, reason,
                    verification_id, verification_no, occurred_at, payload, created_at
                )
                VALUES (
                    :id, :device_code, :staff_id, :client_task_id, :task_type, :status, :reason,
                    :verification_id, :verification_no, :occurred_at, CAST(:payload AS jsonb), :created_at
                )
                ON CONFLICT (device_code, client_task_id) DO UPDATE SET
                    status = terminal_sync_tasks.status
                """, parameterSource.terminalSyncTaskParameters(task));
        return task;
    }

    @Override
    public TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO ticket_price_rules (
                    id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                    holiday_price_cent, discount_rate, channel_id, currency, status, eligibility_rule,
                    created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :variant_type, :price_mode, :weekday_price_cent, :weekend_price_cent,
                    :holiday_price_cent, :discount_rate, :channel_id, :currency, :status, CAST(:eligibility_rule AS jsonb),
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    variant_type = EXCLUDED.variant_type,
                    price_mode = EXCLUDED.price_mode,
                    weekday_price_cent = EXCLUDED.weekday_price_cent,
                    weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent,
                    discount_rate = EXCLUDED.discount_rate,
                    channel_id = EXCLUDED.channel_id,
                    currency = EXCLUDED.currency,
                    status = EXCLUDED.status,
                    eligibility_rule = EXCLUDED.eligibility_rule,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.priceRuleParameters(rule));
        return rule;
    }

    @Override
    public List<TicketPriceRuleRecord> listPriceRules() {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, discount_rate, channel_id, currency, status,
                       eligibility_rule::text AS eligibility_rule, created_at, updated_at
                  FROM ticket_price_rules
                 ORDER BY created_at DESC, id
                """, rowMapper::mapPriceRule);
    }

    @Override
    public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, discount_rate, channel_id, currency, status,
                       eligibility_rule::text AS eligibility_rule, created_at, updated_at
                  FROM ticket_price_rules
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapPriceRule).stream().findFirst();
    }


    @Override
    public TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO ticket_dynamic_pricing_rules (
                    id, ticket_type_id, rule_name, priority, effective_from, effective_to, sales_channel,
                    adjustment_type, adjustment_rate, adjustment_amount_cent, min_price_cent, max_price_cent,
                    status, condition_rule_json, operator_note, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :rule_name, :priority, :effective_from, :effective_to, :sales_channel,
                    :adjustment_type, :adjustment_rate, :adjustment_amount_cent, :min_price_cent, :max_price_cent,
                    :status, :condition_rule_json::jsonb, :operator_note, :evidence_attachment_ids::jsonb, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    rule_name = EXCLUDED.rule_name,
                    priority = EXCLUDED.priority,
                    effective_from = EXCLUDED.effective_from,
                    effective_to = EXCLUDED.effective_to,
                    sales_channel = EXCLUDED.sales_channel,
                    adjustment_type = EXCLUDED.adjustment_type,
                    adjustment_rate = EXCLUDED.adjustment_rate,
                    adjustment_amount_cent = EXCLUDED.adjustment_amount_cent,
                    min_price_cent = EXCLUDED.min_price_cent,
                    max_price_cent = EXCLUDED.max_price_cent,
                    status = EXCLUDED.status,
                    condition_rule_json = EXCLUDED.condition_rule_json,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.dynamicPricingRuleParameters(rule));
        return rule;
    }

    @Override
    public List<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
        return jdbcTemplate.query(JdbcTicketingSql.dynamicPricingRuleSelect() + " ORDER BY priority DESC, updated_at DESC, id",
                rowMapper::mapDynamicPricingRule);
    }

    @Override
    public Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.dynamicPricingRuleSelect() + " WHERE id = :id", Map.of("id", id),
                rowMapper::mapDynamicPricingRule).stream().findFirst();
    }

    @Override
    public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (
                    id, order_no, customer_id, ticket_type_id, price_rule_id, quantity, visit_date,
                    sales_channel, buyer_phone, amount_cent, dynamic_pricing_rule_id, base_amount_cent,
                    dynamic_adjustment_cent, currency, pay_status, issue_status, status, created_at, updated_at,
                    merchant_id
                )
                VALUES (
                    :id, :order_no, :customer_id, :ticket_type_id, :price_rule_id, :quantity, :visit_date,
                    :sales_channel, :buyer_phone, :amount_cent, :dynamic_pricing_rule_id, :base_amount_cent,
                    :dynamic_adjustment_cent, :currency, :pay_status, :issue_status, :status, :created_at, :updated_at,
                    :merchant_id
                )
                -- merchant_id（下单受理商户）下单时定，后续 update（出票/置 paid）不覆盖。
                ON CONFLICT (id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    price_rule_id = EXCLUDED.price_rule_id,
                    quantity = EXCLUDED.quantity,
                    visit_date = EXCLUDED.visit_date,
                    sales_channel = EXCLUDED.sales_channel,
                    buyer_phone = EXCLUDED.buyer_phone,
                    amount_cent = EXCLUDED.amount_cent,
                    dynamic_pricing_rule_id = EXCLUDED.dynamic_pricing_rule_id,
                    base_amount_cent = EXCLUDED.base_amount_cent,
                    dynamic_adjustment_cent = EXCLUDED.dynamic_adjustment_cent,
                    currency = EXCLUDED.currency,
                    pay_status = EXCLUDED.pay_status,
                    issue_status = EXCLUDED.issue_status,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketOrderParameters(order));
        return order;
    }

    @Override
    public Optional<TicketOrderRecord> findTicketOrderById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " WHERE id = :id", Map.of("id", id), rowMapper::mapTicketOrder)
                .stream()
                .findFirst();
    }

    @Override
    public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
        return saveTicketOrder(order);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrders() {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + " ORDER BY created_at DESC, id", rowMapper::mapTicketOrder);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id
                """, Map.of("customer_id", customerId), rowMapper::mapTicketOrder);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), rowMapper::mapTicketOrder);
    }

    @Override
    public int atomicCancelUnpaidOrder(String orderId, java.time.Instant now) {
        // 2a-5：原子取消未支付订单（仅 created+unpaid 可取消）。返回 1 时调用方才释放库存，防并发重复释放/释放已付订单库存。
        return jdbcTemplate.update("""
                UPDATE ticket_orders
                   SET status = 'cancelled',
                       updated_at = :now
                 WHERE id = :id
                   AND pay_status = 'unpaid'
                   AND status = 'created'
                """, new MapSqlParameterSource()
                .addValue("id", orderId)
                .addValue("now", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff) {
        // 2a-5：超时未支付订单（created+unpaid 且 created_at < cutoff），供定时任务释放库存。
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE status = 'created'
                   AND pay_status = 'unpaid'
                   AND created_at < :cutoff
                 ORDER BY created_at ASC, id
                """, Map.of("cutoff", cutoff.atOffset(java.time.ZoneOffset.UTC)), rowMapper::mapTicketOrder);
    }

    @Override
    public TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent) {
        jdbcTemplate.update("""
                INSERT INTO ticket_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency, :channel,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketPaymentIntentParameters(intent));
        return intent;
    }

    @Override
    public Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketPaymentIntentSelect() + " WHERE id = :id",
                Map.of("id", id), rowMapper::mapTicketPaymentIntent).stream().findFirst();
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlements() {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + " ORDER BY created_at DESC, id", rowMapper::mapIssuedEntitlement);
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + """
                 WHERE order_id = :order_id
                 ORDER BY created_at ASC, id
                """, Map.of("order_id", orderId), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + " WHERE id = :id", Map.of("id", id), rowMapper::mapIssuedEntitlement)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Long> findBarProductInternalPrice(String barProductId) {
        // 结算重构批3-1(PRD §6.1)：实物权益按酒吧商品内部结算价(V152)结算。
        // 直接读 bar_products 单列(不引 bar 域类型，避免 bar↔ticketing 包环)；无此商品/内部价为空 → 空 → 判定层回退 value_amount。
        return jdbcTemplate.query("""
                SELECT internal_settlement_price_cent
                  FROM bar_products
                 WHERE id = :id
                """, Map.of("id", barProductId),
                (resultSet, rowNumber) -> {
                    long value = resultSet.getLong("internal_settlement_price_cent");
                    return resultSet.wasNull() ? null : value;
                }).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    @Override
    public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
        jdbcTemplate.update("""
                INSERT INTO ticket_credentials (
                    id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                    issued_at, expires_at, status, refreshed_count, created_at, updated_at, refresh_interval_seconds,
                    window_started_at, signature_value, signature_version
                )
                VALUES (
                    :id, :entitlement_id, :order_id, :credential_token, :nonce, :credential_hash, :credential_type,
                    :issued_at, :expires_at, :status, :refreshed_count, :created_at, :updated_at, :refresh_interval_seconds,
                    :window_started_at, :signature_value, :signature_version
                )
                ON CONFLICT (id) DO UPDATE SET
                    credential_token = NULL,
                    nonce = EXCLUDED.nonce,
                    credential_hash = EXCLUDED.credential_hash,
                    credential_type = EXCLUDED.credential_type,
                    issued_at = EXCLUDED.issued_at,
                    expires_at = EXCLUDED.expires_at,
                    status = EXCLUDED.status,
                    refreshed_count = EXCLUDED.refreshed_count,
                    updated_at = EXCLUDED.updated_at,
                    refresh_interval_seconds = EXCLUDED.refresh_interval_seconds,
                    window_started_at = EXCLUDED.window_started_at,
                    signature_value = EXCLUDED.signature_value,
                    signature_version = EXCLUDED.signature_version
                """, parameterSource.ticketCredentialParameters(credential));
        return credential;
    }

    @Override
    public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                       issued_at, expires_at, status, refreshed_count, created_at, updated_at, refresh_interval_seconds,
                       window_started_at, signature_value, signature_version
                  FROM ticket_credentials
                 WHERE credential_hash = :credential_hash
                """, Map.of("credential_hash", parameterSource.credentialTokenHash(credentialToken)), rowMapper::mapTicketCredential).stream().findFirst();
    }

    @Override
    public TicketRefundRecord saveTicketRefund(TicketRefundRecord refund) {
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (
                    id, target_id, refund_amount_cent, reason, refund_channel, status,
                    requested_by, approved_by, approved_at, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :target_id, :refund_amount_cent, :reason, :refund_channel, :status,
                    :requested_by, :approved_by, :approved_at, :created_at, :updated_at, :merchant_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketRefundParameters(refund));
        return refund;
    }

    @Override
    public Optional<TicketRefundRecord> findTicketRefundById(String id) {
        return jdbcTemplate.query("""
                SELECT id, target_id, refund_amount_cent, reason, refund_channel, status,
                       requested_by, approved_by, approved_at, created_at, updated_at, merchant_id
                  FROM ticket_refunds
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTicketRefund).stream().findFirst();
    }

    @Override
    public TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line) {
        jdbcTemplate.update("""
                INSERT INTO ticket_refund_lines (
                    id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                    refund_amount_cent, remaining_times_before, used_times_before,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :refund_id, :order_id, :entitlement_id, :entitlement_type, :line_type,
                    :refund_amount_cent, :remaining_times_before, :used_times_before,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    refund_amount_cent = EXCLUDED.refund_amount_cent,
                    remaining_times_before = EXCLUDED.remaining_times_before,
                    used_times_before = EXCLUDED.used_times_before,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketRefundLineParameters(line));
        return line;
    }

    @Override
    public List<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
        return jdbcTemplate.query("""
                SELECT id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                       refund_amount_cent, remaining_times_before, used_times_before,
                       status, created_at, updated_at
                  FROM ticket_refund_lines
                 WHERE refund_id = :refund_id
                 ORDER BY created_at ASC, id
                """, Map.of("refund_id", refundId), rowMapper::mapTicketRefundLine);
    }

    @Override
    public long sumPendingRefundAmountByOrderId(String orderId) {
        Long result = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM ticket_refunds
                 WHERE target_id = :order_id
                   AND status IN ('requested', 'approved')
                """, Map.of("order_id", orderId), Long.class);
        return result == null ? 0L : result;
    }

    @Override
    public long sumApprovedRefundAmountByOrderId(String orderId) {
        Long result = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM ticket_refunds
                 WHERE target_id = :order_id
                   AND status = 'approved'
                """, Map.of("order_id", orderId), Long.class);
        return result == null ? 0L : result;
    }

    @Override
    public Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
        // 2a-3 退款冻结：核销必须 status='active'（防退款冻结后经游乐 gateway 直连 atomicUseEntitlement 绕过 service 检查）。
        int rowCount = jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = used_times + 1,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND used_times < total_times
                   AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
        if (rowCount == 0) {
            return Optional.empty();
        }
        return findIssuedEntitlementById(id);
    }

    @Override
    public Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt) {
        // R5 T5 核销冲正：回滚一次核销（used_times-1，下限 0）；曾用满（status 非 active）则恢复 active。
        // 2a-3 退款冻结：status='refunded'（退款审批冻结）不可被冲正解冻 → WHERE 排除，避免"退款后冲正解冻再核销"。
        int rowCount = jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = GREATEST(used_times - 1, 0),
                       status = CASE WHEN status <> 'active' THEN 'active' ELSE status END,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND used_times > 0
                   AND status <> 'refunded'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
        if (rowCount == 0) {
            return Optional.empty();
        }
        return findIssuedEntitlementById(id);
    }

    @Override
    public int freezeEntitlementForRefund(String id, java.time.Instant updatedAt) {
        // 2a-3 退款冻结：退款审批通过 → 把未冻结的 active 权益置 refunded，使其不可再核销（含游乐 gateway 路径）。
        // 仅冻结 active（幂等：已 refunded 返回 0）。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET status = 'refunded',
                       updated_at = :updated_at
                 WHERE id = :id
                   AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
        // F-TXN-03 到期未用：valid_to 已过、仍 active、且 used_times=0 的权益（到期自动退款 / 作废候选）。
        return jdbcTemplate.query(JdbcTicketingSql.issuedEntitlementSelect() + """
                 WHERE valid_to < :now
                   AND status = 'active'
                   AND used_times = 0
                 ORDER BY order_id, created_at ASC, id
                """, Map.of("now", now.atOffset(java.time.ZoneOffset.UTC)), rowMapper::mapIssuedEntitlement);
    }

    @Override
    public int forfeitEntitlement(String id, java.time.Instant now) {
        // F-TXN-03 一张用后其余作废：到期未用权益置 forfeited（仅 active 且 used_times=0，幂等：已非此态返回 0）。
        return jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET status = 'forfeited',
                       updated_at = :updated_at
                 WHERE id = :id
                   AND status = 'active'
                   AND used_times = 0
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", now.atOffset(java.time.ZoneOffset.UTC)));
    }

    @Override
    public boolean tryLockOrderForIssuance(String orderId) {
        return jdbcTemplate.update("""
                UPDATE ticket_orders
                   SET issue_status = 'issuing'
                 WHERE id = :id
                   AND issue_status = 'not_issued'
                """, Map.of("id", orderId)) > 0;
    }

    @Override
    public String findLatestFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, Map.of(), String.class);
    }


}
