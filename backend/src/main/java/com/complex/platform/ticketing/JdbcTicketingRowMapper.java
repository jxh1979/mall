package com.complex.platform.ticketing;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class JdbcTicketingRowMapper {

    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final com.complex.platform.common.crypto.PiiCipher piiCipher;

    JdbcTicketingRowMapper(ObjectMapper objectMapper, com.complex.platform.common.crypto.PiiCipher piiCipher) {
        this.objectMapper = objectMapper;
        this.piiCipher = piiCipher;
    }

    TicketTypeRecord mapTicketType(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketTypeRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_name"),
                resultSet.getString("base_calendar_price_mode"),
                nullableLong(resultSet, "weekday_price_cent"),
                nullableLong(resultSet, "weekend_price_cent"),
                nullableLong(resultSet, "holiday_price_cent"),
                resultSet.getBoolean("stock_required"),
                resultSet.getString("status"),
                nullableInteger(resultSet, "validity_days"),
                nullableInteger(resultSet, "refresh_interval_seconds"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                nullableInteger(resultSet, "stock_quantity"),
                resultSet.getInt("sold_quantity"),
                resultSet.getString("ticket_category"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason"),
                resultSet.getBoolean("refundable"),
                resultSet.getString("bar_id")
        );
    }

    EntitlementTemplateRecord mapEntitlementTemplate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EntitlementTemplateRecord(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("entitlement_type"),
                resultSet.getInt("default_total_times"),
                resultSet.getLong("default_value_cent"),
                resultSet.getString("status"),
                resultSet.getString("merchant_id"),
                resultSet.getString("venue_id"),
                resultSet.getString("point_id"),
                resultSet.getString("bar_item_ref"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("entitlement_kind"),
                nullableLong(resultSet, "agreement_price_cent"),
                resultSet.getString("consumption_window"),
                resultSet.getString("exclusive_merchant_id"),
                resultSet.getBoolean("require_face_photo"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason")
        );
    }

    TicketTypeEntitlementRecord mapTicketTypeEntitlement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketTypeEntitlementRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("entitlement_template_id"),
                resultSet.getInt("quantity"),
                resultSet.getInt("total_times"),
                nullableLong(resultSet, "value_amount_cent"),
                resultSet.getString("effective_rule"),
                resultSet.getInt("num_people"),
                nullableLong(resultSet, "component_price_cent"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    IssuedEntitlementRecord mapIssuedEntitlement(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "updated_at"),
                resultSet.getInt("num_people"),
                nullableLong(resultSet, "allocated_revenue_cent"),
                resultSet.getString("entitlement_kind"),
                nullableLong(resultSet, "agreement_price_cent"),
                resultSet.getString("consumption_window"),
                resultSet.getString("exclusive_merchant_id"),
                resultSet.getString("parent_ticket_id"),
                resultSet.getInt("slot_index"),
                resultSet.getString("bar_id"),
                nullableLong(resultSet, "component_full_price_cent")
        );
    }

    TicketInstanceRecord mapTicketInstance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketInstanceRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_no"),
                resultSet.getString("order_id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getInt("seq_in_order"),
                resultSet.getInt("num_people"),
                resultSet.getInt("total_times"),
                resultSet.getString("buyer_customer_id"),
                resultSet.getString("current_holder_customer_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "first_used_at"),
                resultSet.getString("first_session_key"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("bar_id")
        );
    }

    VerificationRecord mapVerification(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "created_at"),
                resultSet.getString("nonce"),
                resultSet.getString("staff_id"),
                resultSet.getString("merchant_id"),
                nullableBoolean(resultSet, "window_valid"),
                nullableBoolean(resultSet, "signature_valid"),
                resultSet.getInt("verified_people"),
                resultSet.getString("reverses_verification_id"),
                resultSet.getString("reversed_by"),
                nullableInstant(resultSet, "reversed_at"),
                nullableLong(resultSet, "settlement_price_cent"),
                resultSet.getString("amusement_verification_id")
        );
    }

    TerminalSyncTaskRecord mapTerminalSyncTask(ResultSet resultSet, int rowNumber) throws SQLException {
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

    TicketPriceRuleRecord mapPriceRule(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "updated_at"),
                nullableInstant(resultSet, "archived_at"),
                resultSet.getString("archived_by"),
                resultSet.getString("delete_status"),
                resultSet.getString("delete_requested_by"),
                nullableInstant(resultSet, "delete_requested_at"),
                resultSet.getString("delete_reason")
        );
    }

    TicketDynamicPricingRuleRecord mapDynamicPricingRule(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketDynamicPricingRuleRecord(
                resultSet.getString("id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("rule_name"),
                resultSet.getInt("priority"),
                resultSet.getObject("effective_from", LocalDate.class),
                resultSet.getObject("effective_to", LocalDate.class),
                resultSet.getString("sales_channel"),
                resultSet.getString("adjustment_type"),
                nullableBigDecimal(resultSet, "adjustment_rate"),
                nullableLong(resultSet, "adjustment_amount_cent"),
                nullableLong(resultSet, "min_price_cent"),
                nullableLong(resultSet, "max_price_cent"),
                resultSet.getString("status"),
                readObjectMap(resultSet.getString("condition_rule_json")),
                resultSet.getString("operator_note"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    TicketOrderRecord mapTicketOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketOrderRecord(
                resultSet.getString("id"),
                resultSet.getString("order_no"),
                resultSet.getString("customer_id"),
                resultSet.getString("ticket_type_id"),
                resultSet.getString("price_rule_id"),
                resultSet.getInt("quantity"),
                resultSet.getObject("visit_date", LocalDate.class),
                resultSet.getString("sales_channel"),
                piiCipher.decrypt(resultSet.getString("buyer_phone")),
                resultSet.getLong("amount_cent"),
                resultSet.getString("dynamic_pricing_rule_id"),
                resultSet.getLong("base_amount_cent"),
                resultSet.getLong("dynamic_adjustment_cent"),
                resultSet.getString("currency"),
                resultSet.getString("pay_status"),
                resultSet.getString("issue_status"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                nullableInstant(resultSet, "paid_at"),
                resultSet.getString("bar_id")
        );
    }

    TicketPaymentIntentRecord mapTicketPaymentIntent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketPaymentIntentRecord(
                resultSet.getString("id"),
                resultSet.getString("business_order_type"),
                resultSet.getString("business_order_id"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("currency"),
                resultSet.getString("channel"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at")
        );
    }

    TicketCredentialRecord mapTicketCredential(ResultSet resultSet, int rowNumber) throws SQLException {
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
                nullableInstant(resultSet, "updated_at"),
                resultSet.getInt("refresh_interval_seconds"),
                nullableInstant(resultSet, "window_started_at"),
                resultSet.getString("signature_value"),
                resultSet.getString("signature_version")
        );
    }

    TicketRefundRecord mapTicketRefund(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TicketRefundRecord(
                resultSet.getString("id"),
                resultSet.getString("target_id"),
                resultSet.getLong("refund_amount_cent"),
                resultSet.getString("reason"),
                resultSet.getString("refund_channel"),
                resultSet.getString("status"),
                resultSet.getString("requested_by"),
                resultSet.getString("approved_by"),
                nullableInstant(resultSet, "approved_at"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("merchant_id")
        );
    }

    TicketRefundLineRecord mapTicketRefundLine(ResultSet resultSet, int rowNumber) throws SQLException {
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

    private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private Boolean nullableBoolean(ResultSet resultSet, String columnName) throws SQLException {
        boolean value = resultSet.getBoolean(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private BigDecimal nullableBigDecimal(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getBigDecimal(columnName);
    }


    private Map<String, String> readStringMap(String value) {
        try {
            return value == null ? Map.of() : objectMapper.readValue(value, STRING_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("terminal sync payload is not readable", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticketing JSON list is not readable", exception);
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
