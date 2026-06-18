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
                    holiday_price_cent, stock_required, status, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_name, :base_calendar_price_mode, :weekday_price_cent, :weekend_price_cent,
                    :holiday_price_cent, :stock_required, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_name = EXCLUDED.ticket_name,
                    base_calendar_price_mode = EXCLUDED.base_calendar_price_mode,
                    weekday_price_cent = EXCLUDED.weekday_price_cent,
                    weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent,
                    stock_required = EXCLUDED.stock_required,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeParameters(ticketType));
        return ticketType;
    }

    @Override
    public List<TicketTypeRecord> listTicketTypes() {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, created_at, updated_at
                  FROM ticket_types
                 ORDER BY created_at DESC, id
                """, rowMapper::mapTicketType);
    }

    @Override
    public Optional<TicketTypeRecord> findTicketTypeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, created_at, updated_at
                  FROM ticket_types
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTicketType).stream().findFirst();
    }

    @Override
    public EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template) {
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (
                    id, name, entitlement_type, default_total_times, default_value_cent,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :name, :entitlement_type, :default_total_times, :default_value_cent,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    entitlement_type = EXCLUDED.entitlement_type,
                    default_total_times = EXCLUDED.default_total_times,
                    default_value_cent = EXCLUDED.default_value_cent,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.entitlementTemplateParameters(template));
        return template;
    }

    @Override
    public List<EntitlementTemplateRecord> listEntitlementTemplates() {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, created_at, updated_at
                  FROM entitlement_templates
                 ORDER BY created_at DESC, id
                """, rowMapper::mapEntitlementTemplate);
    }

    @Override
    public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, created_at, updated_at
                  FROM entitlement_templates
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapEntitlementTemplate).stream().findFirst();
    }

    @Override
    public TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                INSERT INTO ticket_type_entitlements (
                    id, ticket_type_id, entitlement_template_id, quantity, total_times,
                    value_amount_cent, effective_rule, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :entitlement_template_id, :quantity, :total_times,
                    :value_amount_cent, :effective_rule, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ticket_type_id = EXCLUDED.ticket_type_id,
                    entitlement_template_id = EXCLUDED.entitlement_template_id,
                    quantity = EXCLUDED.quantity,
                    total_times = EXCLUDED.total_times,
                    value_amount_cent = EXCLUDED.value_amount_cent,
                    effective_rule = EXCLUDED.effective_rule,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.ticketTypeEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, entitlement_template_id, quantity, total_times,
                       value_amount_cent, effective_rule, created_at, updated_at
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
                    value_amount_cent, status, valid_from, valid_to, created_at, updated_at
                )
                VALUES (
                    :id, :ticket_type_id, :order_id, :entitlement_template_id, :entitlement_code,
                    :entitlement_type, :merchant_id, :venue_id, :point_id, :total_times, :used_times,
                    :value_amount_cent, :status, :valid_from, :valid_to, :created_at, :updated_at
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
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.issuedEntitlementParameters(entitlement));
        return entitlement;
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at
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
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at
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
                    client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid
                )
                VALUES (
                    :id, :verification_no, :entitlement_id, :entitlement_code, :credential_type,
                    :credential_payload, :device_id, :point_id, :status, :result_reason, :source_type,
                    :client_task_id, :created_at, :nonce, :staff_id, :merchant_id, :window_valid, :signature_valid
                )
                """, parameterSource.verificationParameters(verification));
        return verification;
    }

    @Override
    public List<VerificationRecord> listVerifications() {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at, nonce, staff_id, merchant_id, window_valid, signature_valid
                  FROM ticket_verifications
                 ORDER BY created_at DESC, id
                """, rowMapper::mapVerification);
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
                    dynamic_adjustment_cent, currency, pay_status, issue_status, status, created_at, updated_at
                )
                VALUES (
                    :id, :order_no, :customer_id, :ticket_type_id, :price_rule_id, :quantity, :visit_date,
                    :sales_channel, :buyer_phone, :amount_cent, :dynamic_pricing_rule_id, :base_amount_cent,
                    :dynamic_adjustment_cent, :currency, :pay_status, :issue_status, :status, :created_at, :updated_at
                )
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
    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return jdbcTemplate.query(JdbcTicketingSql.ticketOrderSelect() + """
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id
                """, Map.of("customer_id", customerId), rowMapper::mapTicketOrder);
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
                    approved_by, approved_at, created_at, updated_at
                )
                VALUES (
                    :id, :target_id, :refund_amount_cent, :reason, :refund_channel, :status,
                    :approved_by, :approved_at, :created_at, :updated_at
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
                       approved_by, approved_at, created_at, updated_at
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
    public Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt) {
        int rowCount = jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = used_times + 1,
                       updated_at = :updated_at
                 WHERE id = :id
                   AND used_times < total_times
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updated_at", updatedAt.atOffset(java.time.ZoneOffset.UTC)));
        if (rowCount == 0) {
            return Optional.empty();
        }
        return findIssuedEntitlementById(id);
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
