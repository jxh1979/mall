package com.complex.platform.ticketing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTicketingRepository implements TicketingRepository {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcTicketingRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
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
                """, ticketTypeParameters(ticketType));
        return ticketType;
    }

    @Override
    public List<TicketTypeRecord> listTicketTypes() {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, created_at, updated_at
                  FROM ticket_types
                 ORDER BY created_at DESC, id
                """, this::mapTicketType);
    }

    @Override
    public Optional<TicketTypeRecord> findTicketTypeById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_name, base_calendar_price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, stock_required, status, created_at, updated_at
                  FROM ticket_types
                 WHERE id = :id
                """, Map.of("id", id), this::mapTicketType).stream().findFirst();
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
                """, entitlementTemplateParameters(template));
        return template;
    }

    @Override
    public List<EntitlementTemplateRecord> listEntitlementTemplates() {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, created_at, updated_at
                  FROM entitlement_templates
                 ORDER BY created_at DESC, id
                """, this::mapEntitlementTemplate);
    }

    @Override
    public Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id) {
        return jdbcTemplate.query("""
                SELECT id, name, entitlement_type, default_total_times, default_value_cent,
                       status, created_at, updated_at
                  FROM entitlement_templates
                 WHERE id = :id
                """, Map.of("id", id), this::mapEntitlementTemplate).stream().findFirst();
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
                """, ticketTypeEntitlementParameters(entitlement));
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
                """, Map.of("ticket_type_id", ticketTypeId), this::mapTicketTypeEntitlement);
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
                """, issuedEntitlementParameters(entitlement));
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
                """, Map.of("entitlement_code", entitlementCode), this::mapIssuedEntitlement).stream().findFirst();
    }

    @Override
    public IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement) {
        jdbcTemplate.update("""
                UPDATE issued_entitlements
                   SET used_times = :used_times,
                       status = :status,
                       updated_at = :updated_at
                 WHERE id = :id
                """, issuedEntitlementParameters(entitlement));
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
                   AND (:merchant_id IS NULL OR merchant_id = :merchant_id OR merchant_id IS NULL)
                   AND (:venue_id IS NULL OR venue_id = :venue_id OR venue_id IS NULL)
                 ORDER BY updated_at DESC, id
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("venue_id", venueId)
                .addValue("limit", limit), (resultSet, rowNumber) ->
                TicketingService.toOfflineEntitlementResponse(mapIssuedEntitlement(resultSet, rowNumber)));
    }

    @Override
    public VerificationRecord saveVerification(VerificationRecord verification) {
        jdbcTemplate.update("""
                INSERT INTO ticket_verifications (
                    id, verification_no, entitlement_id, entitlement_code, credential_type,
                    credential_payload, device_id, point_id, status, result_reason, source_type,
                    client_task_id, created_at
                )
                VALUES (
                    :id, :verification_no, :entitlement_id, :entitlement_code, :credential_type,
                    :credential_payload, :device_id, :point_id, :status, :result_reason, :source_type,
                    :client_task_id, :created_at
                )
                """, verificationParameters(verification));
        return verification;
    }

    @Override
    public List<VerificationRecord> listVerifications() {
        return jdbcTemplate.query("""
                SELECT id, verification_no, entitlement_id, entitlement_code, credential_type,
                       credential_payload, device_id, point_id, status, result_reason, source_type,
                       client_task_id, created_at
                  FROM ticket_verifications
                 ORDER BY created_at DESC, id
                """, this::mapVerification);
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
        ), this::mapTerminalSyncTask).stream().findFirst();
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
                """, terminalSyncTaskParameters(task));
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
                """, priceRuleParameters(rule));
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
                """, this::mapPriceRule);
    }

    @Override
    public Optional<TicketPriceRuleRecord> findPriceRuleById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ticket_type_id, variant_type, price_mode, weekday_price_cent, weekend_price_cent,
                       holiday_price_cent, discount_rate, channel_id, currency, status,
                       eligibility_rule::text AS eligibility_rule, created_at, updated_at
                  FROM ticket_price_rules
                 WHERE id = :id
                """, Map.of("id", id), this::mapPriceRule).stream().findFirst();
    }

    @Override
    public TicketOrderRecord saveTicketOrder(TicketOrderRecord order) {
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (
                    id, order_no, customer_id, ticket_type_id, price_rule_id, quantity, visit_date,
                    sales_channel, buyer_phone, amount_cent, currency, pay_status, issue_status,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :order_no, :customer_id, :ticket_type_id, :price_rule_id, :quantity, :visit_date,
                    :sales_channel, :buyer_phone, :amount_cent, :currency, :pay_status, :issue_status,
                    :status, :created_at, :updated_at
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
                    currency = EXCLUDED.currency,
                    pay_status = EXCLUDED.pay_status,
                    issue_status = EXCLUDED.issue_status,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, ticketOrderParameters(order));
        return order;
    }

    @Override
    public Optional<TicketOrderRecord> findTicketOrderById(String id) {
        return jdbcTemplate.query(ticketOrderSelect() + " WHERE id = :id", Map.of("id", id), this::mapTicketOrder)
                .stream()
                .findFirst();
    }

    @Override
    public TicketOrderRecord updateTicketOrder(TicketOrderRecord order) {
        return saveTicketOrder(order);
    }

    @Override
    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return jdbcTemplate.query(ticketOrderSelect() + """
                 WHERE customer_id = :customer_id
                 ORDER BY created_at DESC, id
                """, Map.of("customer_id", customerId), this::mapTicketOrder);
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
                """, ticketPaymentIntentParameters(intent));
        return intent;
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlements() {
        return jdbcTemplate.query(issuedEntitlementSelect() + " ORDER BY created_at DESC, id", this::mapIssuedEntitlement);
    }

    @Override
    public List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId) {
        return jdbcTemplate.query(issuedEntitlementSelect() + """
                 WHERE order_id = :order_id
                 ORDER BY created_at ASC, id
                """, Map.of("order_id", orderId), this::mapIssuedEntitlement);
    }

    @Override
    public Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id) {
        return jdbcTemplate.query(issuedEntitlementSelect() + " WHERE id = :id", Map.of("id", id), this::mapIssuedEntitlement)
                .stream()
                .findFirst();
    }

    @Override
    public TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential) {
        jdbcTemplate.update("""
                INSERT INTO ticket_credentials (
                    id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                    issued_at, expires_at, status, refreshed_count, created_at, updated_at
                )
                VALUES (
                    :id, :entitlement_id, :order_id, :credential_token, :nonce, :credential_hash, :credential_type,
                    :issued_at, :expires_at, :status, :refreshed_count, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    credential_token = EXCLUDED.credential_token,
                    nonce = EXCLUDED.nonce,
                    credential_hash = EXCLUDED.credential_hash,
                    credential_type = EXCLUDED.credential_type,
                    issued_at = EXCLUDED.issued_at,
                    expires_at = EXCLUDED.expires_at,
                    status = EXCLUDED.status,
                    refreshed_count = EXCLUDED.refreshed_count,
                    updated_at = EXCLUDED.updated_at
                """, ticketCredentialParameters(credential));
        return credential;
    }

    @Override
    public Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken) {
        return jdbcTemplate.query("""
                SELECT id, entitlement_id, order_id, credential_token, nonce, credential_hash, credential_type,
                       issued_at, expires_at, status, refreshed_count, created_at, updated_at
                  FROM ticket_credentials
                 WHERE credential_token = :credential_token
                """, Map.of("credential_token", credentialToken), this::mapTicketCredential).stream().findFirst();
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
                """, ticketRefundParameters(refund));
        return refund;
    }

    @Override
    public Optional<TicketRefundRecord> findTicketRefundById(String id) {
        return jdbcTemplate.query("""
                SELECT id, target_id, refund_amount_cent, reason, refund_channel, status,
                       approved_by, approved_at, created_at, updated_at
                  FROM ticket_refunds
                 WHERE id = :id
                """, Map.of("id", id), this::mapTicketRefund).stream().findFirst();
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
                """, ticketRefundLineParameters(line));
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
                """, Map.of("refund_id", refundId), this::mapTicketRefundLine);
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

    private MapSqlParameterSource ticketTypeParameters(TicketTypeRecord ticketType) {
        return new MapSqlParameterSource()
                .addValue("id", ticketType.id())
                .addValue("ticket_name", ticketType.ticketName())
                .addValue("base_calendar_price_mode", ticketType.baseCalendarPriceMode())
                .addValue("weekday_price_cent", ticketType.weekdayPriceCent())
                .addValue("weekend_price_cent", ticketType.weekendPriceCent())
                .addValue("holiday_price_cent", ticketType.holidayPriceCent())
                .addValue("stock_required", ticketType.stockRequired())
                .addValue("status", ticketType.status())
                .addValue("created_at", timestampWithTimeZone(ticketType.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ticketType.updatedAt()));
    }

    private MapSqlParameterSource entitlementTemplateParameters(EntitlementTemplateRecord template) {
        return new MapSqlParameterSource()
                .addValue("id", template.id())
                .addValue("name", template.name())
                .addValue("entitlement_type", template.entitlementType())
                .addValue("default_total_times", template.defaultTotalTimes())
                .addValue("default_value_cent", template.defaultValueCent())
                .addValue("status", template.status())
                .addValue("created_at", timestampWithTimeZone(template.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(template.updatedAt()));
    }

    private MapSqlParameterSource ticketTypeEntitlementParameters(TicketTypeEntitlementRecord entitlement) {
        return new MapSqlParameterSource()
                .addValue("id", entitlement.id())
                .addValue("ticket_type_id", entitlement.ticketTypeId())
                .addValue("entitlement_template_id", entitlement.entitlementTemplateId())
                .addValue("quantity", entitlement.quantity())
                .addValue("total_times", entitlement.totalTimes())
                .addValue("value_amount_cent", entitlement.valueAmountCent())
                .addValue("effective_rule", entitlement.effectiveRule())
                .addValue("created_at", timestampWithTimeZone(entitlement.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(entitlement.updatedAt()));
    }

    private MapSqlParameterSource issuedEntitlementParameters(IssuedEntitlementRecord entitlement) {
        return new MapSqlParameterSource()
                .addValue("id", entitlement.id())
                .addValue("ticket_type_id", entitlement.ticketTypeId())
                .addValue("order_id", entitlement.orderId())
                .addValue("entitlement_template_id", entitlement.entitlementTemplateId())
                .addValue("entitlement_code", entitlement.entitlementCode())
                .addValue("entitlement_type", entitlement.entitlementType())
                .addValue("merchant_id", entitlement.merchantId())
                .addValue("venue_id", entitlement.venueId())
                .addValue("point_id", entitlement.pointId())
                .addValue("total_times", entitlement.totalTimes())
                .addValue("used_times", entitlement.usedTimes())
                .addValue("value_amount_cent", entitlement.valueAmountCent())
                .addValue("status", entitlement.status())
                .addValue("valid_from", timestampWithTimeZone(entitlement.validFrom()))
                .addValue("valid_to", timestampWithTimeZone(entitlement.validTo()))
                .addValue("created_at", timestampWithTimeZone(entitlement.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(entitlement.updatedAt()));
    }

    private MapSqlParameterSource verificationParameters(VerificationRecord verification) {
        return new MapSqlParameterSource()
                .addValue("id", verification.id())
                .addValue("verification_no", verification.verificationNo())
                .addValue("entitlement_id", verification.entitlementId())
                .addValue("entitlement_code", verification.entitlementCode())
                .addValue("credential_type", verification.credentialType())
                .addValue("credential_payload", verification.credentialPayload())
                .addValue("device_id", verification.deviceId())
                .addValue("point_id", verification.pointId())
                .addValue("status", verification.status())
                .addValue("result_reason", verification.resultReason())
                .addValue("source_type", verification.sourceType())
                .addValue("client_task_id", verification.clientTaskId())
                .addValue("created_at", timestampWithTimeZone(verification.createdAt()));
    }

    private MapSqlParameterSource terminalSyncTaskParameters(TerminalSyncTaskRecord task) {
        return new MapSqlParameterSource()
                .addValue("id", task.id())
                .addValue("device_code", task.deviceCode())
                .addValue("staff_id", task.staffId())
                .addValue("client_task_id", task.clientTaskId())
                .addValue("task_type", task.taskType())
                .addValue("status", task.status())
                .addValue("reason", task.reason())
                .addValue("verification_id", task.verificationId())
                .addValue("verification_no", task.verificationNo())
                .addValue("occurred_at", task.occurredAt())
                .addValue("payload", writeJson(task.payload()))
                .addValue("created_at", timestampWithTimeZone(task.createdAt()));
    }

    private MapSqlParameterSource priceRuleParameters(TicketPriceRuleRecord rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("ticket_type_id", rule.ticketTypeId())
                .addValue("variant_type", rule.variantType())
                .addValue("price_mode", rule.priceMode())
                .addValue("weekday_price_cent", rule.weekdayPriceCent())
                .addValue("weekend_price_cent", rule.weekendPriceCent())
                .addValue("holiday_price_cent", rule.holidayPriceCent())
                .addValue("discount_rate", rule.discountRate())
                .addValue("channel_id", rule.channelId())
                .addValue("currency", rule.currency())
                .addValue("status", rule.status())
                .addValue("eligibility_rule", writeJson(rule.eligibilityRule()))
                .addValue("created_at", timestampWithTimeZone(rule.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(rule.updatedAt()));
    }

    private MapSqlParameterSource ticketOrderParameters(TicketOrderRecord order) {
        return new MapSqlParameterSource()
                .addValue("id", order.id())
                .addValue("order_no", order.orderNo())
                .addValue("customer_id", order.customerId())
                .addValue("ticket_type_id", order.ticketTypeId())
                .addValue("price_rule_id", order.priceRuleId())
                .addValue("quantity", order.quantity())
                .addValue("visit_date", order.visitDate())
                .addValue("sales_channel", order.salesChannel())
                .addValue("buyer_phone", order.buyerPhone())
                .addValue("amount_cent", order.amountCent())
                .addValue("currency", order.currency())
                .addValue("pay_status", order.payStatus())
                .addValue("issue_status", order.issueStatus())
                .addValue("status", order.status())
                .addValue("created_at", timestampWithTimeZone(order.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(order.updatedAt()));
    }

    private MapSqlParameterSource ticketPaymentIntentParameters(TicketPaymentIntentRecord intent) {
        return new MapSqlParameterSource()
                .addValue("id", intent.id())
                .addValue("business_order_type", intent.businessOrderType())
                .addValue("business_order_id", intent.businessOrderId())
                .addValue("amount_cent", intent.amountCent())
                .addValue("currency", intent.currency())
                .addValue("channel", intent.channel())
                .addValue("status", intent.status())
                .addValue("created_at", timestampWithTimeZone(intent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(intent.updatedAt()));
    }

    private MapSqlParameterSource ticketCredentialParameters(TicketCredentialRecord credential) {
        return new MapSqlParameterSource()
                .addValue("id", credential.id())
                .addValue("entitlement_id", credential.entitlementId())
                .addValue("order_id", credential.orderId())
                .addValue("credential_token", credential.credentialToken())
                .addValue("nonce", credential.nonce())
                .addValue("credential_hash", credential.credentialHash())
                .addValue("credential_type", credential.credentialType())
                .addValue("issued_at", timestampWithTimeZone(credential.issuedAt()))
                .addValue("expires_at", timestampWithTimeZone(credential.expiresAt()))
                .addValue("status", credential.status())
                .addValue("refreshed_count", credential.refreshedCount())
                .addValue("created_at", timestampWithTimeZone(credential.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(credential.updatedAt()));
    }

    private MapSqlParameterSource ticketRefundParameters(TicketRefundRecord refund) {
        return new MapSqlParameterSource()
                .addValue("id", refund.id())
                .addValue("target_id", refund.targetId())
                .addValue("refund_amount_cent", refund.refundAmountCent())
                .addValue("reason", refund.reason())
                .addValue("refund_channel", refund.refundChannel())
                .addValue("status", refund.status())
                .addValue("approved_by", refund.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(refund.approvedAt()))
                .addValue("created_at", timestampWithTimeZone(refund.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(refund.updatedAt()));
    }

    private MapSqlParameterSource ticketRefundLineParameters(TicketRefundLineRecord line) {
        return new MapSqlParameterSource()
                .addValue("id", line.id())
                .addValue("refund_id", line.refundId())
                .addValue("order_id", line.orderId())
                .addValue("entitlement_id", line.entitlementId())
                .addValue("entitlement_type", line.entitlementType())
                .addValue("line_type", line.lineType())
                .addValue("refund_amount_cent", line.refundAmountCent())
                .addValue("remaining_times_before", line.remainingTimesBefore())
                .addValue("used_times_before", line.usedTimesBefore())
                .addValue("status", line.status())
                .addValue("created_at", timestampWithTimeZone(line.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(line.updatedAt()));
    }

    private TicketTypeRecord mapTicketType(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketTypeRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_name"),
                resultSet.getString("base_calendar_price_mode"),
                nullableLong(resultSet, "weekday_price_cent"),
                nullableLong(resultSet, "weekend_price_cent"),
                nullableLong(resultSet, "holiday_price_cent"),
                resultSet.getBoolean("stock_required"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private EntitlementTemplateRecord mapEntitlementTemplate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EntitlementTemplateRecord(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("entitlement_type"),
                resultSet.getInt("default_total_times"),
                resultSet.getLong("default_value_cent"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TicketTypeEntitlementRecord mapTicketTypeEntitlement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketTypeEntitlementRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("entitlement_template_id"),
                resultSet.getInt("quantity"),
                resultSet.getInt("total_times"),
                nullableLong(resultSet, "value_amount_cent"),
                resultSet.getString("effective_rule"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private IssuedEntitlementRecord mapIssuedEntitlement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new IssuedEntitlementRecord(
                resultSet.getString("id"),
                resultSet.getString("entitlement_code"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("order_id"),
                resultSet.getString("entitlement_template_id"),
                resultSet.getString("entitlement_type"),
                resultSet.getString("merchant_id"),
                resultSet.getString("venue_id"),
                resultSet.getString("point_id"),
                resultSet.getInt("total_times"),
                resultSet.getInt("used_times"),
                nullableLong(resultSet, "value_amount_cent"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "valid_from"),
                nullableInstant(resultSet, "valid_to"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private VerificationRecord mapVerification(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VerificationRecord(
                resultSet.getString("id"),
                resultSet.getString("verification_no"),
                resultSet.getString("entitlement_id"),
                resultSet.getString("entitlement_code"),
                resultSet.getString("credential_type"),
                resultSet.getString("credential_payload"),
                resultSet.getString("device_id"),
                resultSet.getString("point_id"),
                resultSet.getString("status"),
                resultSet.getString("result_reason"),
                resultSet.getString("source_type"),
                resultSet.getString("client_task_id"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private TerminalSyncTaskRecord mapTerminalSyncTask(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TerminalSyncTaskRecord(
                resultSet.getString("id"),
                resultSet.getString("device_code"),
                resultSet.getString("staff_id"),
                resultSet.getString("client_task_id"),
                resultSet.getString("task_type"),
                resultSet.getString("status"),
                resultSet.getString("reason"),
                resultSet.getString("verification_id"),
                resultSet.getString("verification_no"),
                resultSet.getString("occurred_at"),
                readStringMap(resultSet.getString("payload")),
                nullableInstant(resultSet, "created_at")
        );
    }

    private TicketPriceRuleRecord mapPriceRule(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketPriceRuleRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("variant_type"),
                resultSet.getString("price_mode"),
                nullableLong(resultSet, "weekday_price_cent"),
                nullableLong(resultSet, "weekend_price_cent"),
                nullableLong(resultSet, "holiday_price_cent"),
                nullableBigDecimal(resultSet, "discount_rate"),
                resultSet.getString("channel_id"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                readObjectMap(resultSet.getString("eligibility_rule")),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TicketOrderRecord mapTicketOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketOrderRecord(
                resultSet.getString("id"),
                resultSet.getString("order_no"),
                resultSet.getString("customer_id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("price_rule_id"),
                resultSet.getInt("quantity"),
                resultSet.getObject("visit_date", LocalDate.class),
                resultSet.getString("sales_channel"),
                resultSet.getString("buyer_phone"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("pay_status"),
                resultSet.getString("issue_status"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TicketCredentialRecord mapTicketCredential(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketCredentialRecord(
                resultSet.getString("id"),
                resultSet.getString("entitlement_id"),
                resultSet.getString("order_id"),
                resultSet.getString("credential_token"),
                resultSet.getString("nonce"),
                resultSet.getString("credential_hash"),
                resultSet.getString("credential_type"),
                nullableInstant(resultSet, "issued_at"),
                nullableInstant(resultSet, "expires_at"),
                resultSet.getString("status"),
                resultSet.getInt("refreshed_count"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TicketRefundRecord mapTicketRefund(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketRefundRecord(
                resultSet.getString("id"),
                resultSet.getString("target_id"),
                resultSet.getLong("refund_amount_cent"),
                resultSet.getString("reason"),
                resultSet.getString("refund_channel"),
                resultSet.getString("status"),
                resultSet.getString("approved_by"),
                nullableInstant(resultSet, "approved_at"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private TicketRefundLineRecord mapTicketRefundLine(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketRefundLineRecord(
                resultSet.getString("id"),
                resultSet.getString("refund_id"),
                resultSet.getString("order_id"),
                resultSet.getString("entitlement_id"),
                resultSet.getString("entitlement_type"),
                resultSet.getString("line_type"),
                resultSet.getLong("refund_amount_cent"),
                resultSet.getInt("remaining_times_before"),
                resultSet.getInt("used_times_before"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    private String ticketOrderSelect() {
        return """
                SELECT id, order_no, customer_id, ticket_type_id, price_rule_id, quantity, visit_date,
                       sales_channel, buyer_phone, amount_cent, currency, pay_status, issue_status,
                       status, created_at, updated_at
                  FROM ticket_orders
                """;
    }

    private String issuedEntitlementSelect() {
        return """
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at
                  FROM issued_entitlements
                """;
    }

    private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private BigDecimal nullableBigDecimal(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getBigDecimal(columnName);
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticketing JSON value is not serializable", exception);
        }
    }

    private Map<String, String> readStringMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, STRING_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("terminal sync payload is not readable", exception);
        }
    }

    private Map<String, Object> readObjectMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticketing JSON payload is not readable", exception);
        }
    }
}
