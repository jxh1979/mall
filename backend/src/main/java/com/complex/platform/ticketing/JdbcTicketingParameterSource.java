package com.complex.platform.ticketing;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class JdbcTicketingParameterSource {

    private final ObjectMapper objectMapper;

    JdbcTicketingParameterSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MapSqlParameterSource ticketTypeParameters(TicketTypeRecord ticketType) {
        return new MapSqlParameterSource()
                .addValue("id", ticketType.id())
                .addValue("ticket_name", ticketType.ticketName())
                .addValue("base_calendar_price_mode", ticketType.baseCalendarPriceMode())
                .addValue("weekday_price_cent", ticketType.weekdayPriceCent())
                .addValue("weekend_price_cent", ticketType.weekendPriceCent())
                .addValue("holiday_price_cent", ticketType.holidayPriceCent())
                .addValue("stock_required", ticketType.stockRequired())
                .addValue("status", ticketType.status())
                .addValue("validity_days", ticketType.validityDays())
                .addValue("refresh_interval_seconds", ticketType.refreshIntervalSeconds())
                .addValue("stock_quantity", ticketType.stockQuantity())
                .addValue("sold_quantity", ticketType.soldQuantity())
                .addValue("created_at", timestampWithTimeZone(ticketType.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(ticketType.updatedAt()));
    }

    MapSqlParameterSource entitlementTemplateParameters(EntitlementTemplateRecord template) {
        return new MapSqlParameterSource()
                .addValue("id", template.id())
                .addValue("name", template.name())
                .addValue("entitlement_type", template.entitlementType())
                .addValue("default_total_times", template.defaultTotalTimes())
                .addValue("default_value_cent", template.defaultValueCent())
                .addValue("status", template.status())
                .addValue("merchant_id", template.merchantId())
                .addValue("venue_id", template.venueId())
                .addValue("point_id", template.pointId())
                .addValue("bar_item_ref", template.barItemRef())
                .addValue("entitlement_kind", template.entitlementKind())
                .addValue("agreement_price_cent", template.agreementPriceCent())
                .addValue("consumption_window", template.consumptionWindow())
                .addValue("created_at", timestampWithTimeZone(template.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(template.updatedAt()));
    }

    MapSqlParameterSource ticketTypeEntitlementParameters(TicketTypeEntitlementRecord entitlement) {
        return new MapSqlParameterSource()
                .addValue("id", entitlement.id())
                .addValue("ticket_type_id", entitlement.ticketTypeId())
                .addValue("entitlement_template_id", entitlement.entitlementTemplateId())
                .addValue("quantity", entitlement.quantity())
                .addValue("total_times", entitlement.totalTimes())
                .addValue("value_amount_cent", entitlement.valueAmountCent())
                .addValue("effective_rule", entitlement.effectiveRule())
                .addValue("num_people", entitlement.numPeople())
                .addValue("component_price_cent", entitlement.componentPriceCent())
                .addValue("created_at", timestampWithTimeZone(entitlement.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(entitlement.updatedAt()));
    }

    MapSqlParameterSource issuedEntitlementParameters(IssuedEntitlementRecord entitlement) {
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
                .addValue("updated_at", timestampWithTimeZone(entitlement.updatedAt()))
                .addValue("num_people", entitlement.numPeople())
                .addValue("allocated_revenue_cent", entitlement.allocatedRevenueCent())
                .addValue("entitlement_kind", entitlement.entitlementKind())
                .addValue("agreement_price_cent", entitlement.agreementPriceCent())
                .addValue("consumption_window", entitlement.consumptionWindow());
    }

    MapSqlParameterSource verificationParameters(VerificationRecord verification) {
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
                .addValue("created_at", timestampWithTimeZone(verification.createdAt()))
                .addValue("nonce", verification.nonce())
                .addValue("staff_id", verification.staffId())
                .addValue("merchant_id", verification.merchantId())
                .addValue("window_valid", verification.windowValid())
                .addValue("signature_valid", verification.signatureValid())
                .addValue("verified_people", verification.verifiedPeople())
                .addValue("reverses_verification_id", verification.reversesVerificationId())
                .addValue("reversed_by", verification.reversedBy())
                .addValue("reversed_at", timestampWithTimeZone(verification.reversedAt()))
                .addValue("settlement_price_cent", verification.settlementPriceCent());
    }

    MapSqlParameterSource terminalSyncTaskParameters(TerminalSyncTaskRecord task) {
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
                .addValue("payload", writeJson(sanitizedSyncPayload(task.payload())))
                .addValue("created_at", timestampWithTimeZone(task.createdAt()));
    }

    MapSqlParameterSource priceRuleParameters(TicketPriceRuleRecord rule) {
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


    MapSqlParameterSource dynamicPricingRuleParameters(TicketDynamicPricingRuleRecord rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("ticket_type_id", rule.ticketTypeId())
                .addValue("rule_name", rule.ruleName())
                .addValue("priority", rule.priority())
                .addValue("effective_from", rule.effectiveFrom())
                .addValue("effective_to", rule.effectiveTo())
                .addValue("sales_channel", rule.salesChannel())
                .addValue("adjustment_type", rule.adjustmentType())
                .addValue("adjustment_rate", rule.adjustmentRate())
                .addValue("adjustment_amount_cent", rule.adjustmentAmountCent())
                .addValue("min_price_cent", rule.minPriceCent())
                .addValue("max_price_cent", rule.maxPriceCent())
                .addValue("status", rule.status())
                .addValue("condition_rule_json", writeJson(rule.conditionRule()))
                .addValue("operator_note", rule.operatorNote())
                .addValue("evidence_attachment_ids", writeJsonList(rule.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(rule.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(rule.updatedAt()));
    }

    MapSqlParameterSource ticketOrderParameters(TicketOrderRecord order) {
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
                .addValue("dynamic_pricing_rule_id", order.dynamicPricingRuleId())
                .addValue("base_amount_cent", order.baseAmountCent())
                .addValue("dynamic_adjustment_cent", order.dynamicAdjustmentCent())
                .addValue("currency", order.currency())
                .addValue("pay_status", order.payStatus())
                .addValue("issue_status", order.issueStatus())
                .addValue("status", order.status())
                .addValue("created_at", timestampWithTimeZone(order.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(order.updatedAt()))
                .addValue("merchant_id", order.merchantId());
    }

    MapSqlParameterSource ticketPaymentIntentParameters(TicketPaymentIntentRecord intent) {
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

    MapSqlParameterSource ticketCredentialParameters(TicketCredentialRecord credential) {
        return new MapSqlParameterSource()
                .addValue("id", credential.id())
                .addValue("entitlement_id", credential.entitlementId())
                .addValue("order_id", credential.orderId())
                .addValue("credential_token", null)
                .addValue("nonce", credential.nonce())
                .addValue("credential_hash", credentialHash(credential))
                .addValue("credential_type", credential.credentialType())
                .addValue("issued_at", timestampWithTimeZone(credential.issuedAt()))
                .addValue("expires_at", timestampWithTimeZone(credential.expiresAt()))
                .addValue("status", credential.status())
                .addValue("refreshed_count", credential.refreshedCount())
                .addValue("refresh_interval_seconds", credential.refreshIntervalSeconds())
                .addValue("window_started_at", timestampWithTimeZone(credential.windowStartedAt()))
                .addValue("signature_value", credential.signatureValue())
                .addValue("signature_version", credential.signatureVersion())
                .addValue("created_at", timestampWithTimeZone(credential.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(credential.updatedAt()));
    }

    MapSqlParameterSource ticketRefundParameters(TicketRefundRecord refund) {
        return new MapSqlParameterSource()
                .addValue("id", refund.id())
                .addValue("target_id", refund.targetId())
                .addValue("refund_amount_cent", refund.refundAmountCent())
                .addValue("reason", refund.reason())
                .addValue("refund_channel", refund.refundChannel())
                .addValue("status", refund.status())
                .addValue("requested_by", refund.requestedBy())
                .addValue("approved_by", refund.approvedBy())
                .addValue("approved_at", timestampWithTimeZone(refund.approvedAt()))
                .addValue("created_at", timestampWithTimeZone(refund.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(refund.updatedAt()))
                .addValue("merchant_id", refund.merchantId());
    }

    MapSqlParameterSource ticketRefundLineParameters(TicketRefundLineRecord line) {
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


    private String credentialHash(TicketCredentialRecord credential) {
        if (credential.credentialToken() != null && !credential.credentialToken().isBlank()) {
            return sha256(credential.credentialToken());
        }
        return credential.credentialHash();
    }

    private Map<String, String> sanitizedSyncPayload(Map<String, String> payload) {
        return TerminalSyncPayloadSanitizer.sanitize(payload, this::sha256);
    }

    String credentialTokenHash(String value) {
        return sha256(value);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("ticket credential hashing unavailable", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticketing JSON value is not serializable", exception);
        }
    }

    private String writeJsonList(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticketing JSON list is not serializable", exception);
        }
    }

}
