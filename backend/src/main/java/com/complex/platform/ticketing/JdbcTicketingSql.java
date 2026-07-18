package com.complex.platform.ticketing;

final class JdbcTicketingSql {

    private JdbcTicketingSql() {
    }

    static String dynamicPricingRuleSelect() {
        return """
                SELECT id, ticket_type_id, rule_name, priority, effective_from, effective_to, sales_channel,
                       adjustment_type, adjustment_rate, adjustment_amount_cent, min_price_cent, max_price_cent,
                       status, condition_rule_json::text AS condition_rule_json, operator_note,
                       evidence_attachment_ids::text AS evidence_attachment_ids, created_at, updated_at
                  FROM ticket_dynamic_pricing_rules
                """;
    }

    static String ticketOrderSelect() {
        return """
                SELECT id, order_no, customer_id, ticket_type_id, price_rule_id, quantity, visit_date,
                       sales_channel, buyer_phone, amount_cent, dynamic_pricing_rule_id,
                       COALESCE(base_amount_cent, amount_cent) AS base_amount_cent,
                       COALESCE(dynamic_adjustment_cent, 0) AS dynamic_adjustment_cent,
                       currency, pay_status, issue_status, status, created_at, updated_at, merchant_id, paid_at,
                       bar_id
                  FROM ticket_orders
                """;
    }

    static String ticketPaymentIntentSelect() {
        return """
                SELECT id, business_order_type, business_order_id, amount_cent, currency, channel,
                       status, created_at, updated_at
                  FROM ticket_payment_intents
                """;
    }

    static String issuedEntitlementSelect() {
        return """
                SELECT id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                       entitlement_type, merchant_id, venue_id, point_id, total_times, used_times,
                       value_amount_cent, status, valid_from, valid_to, created_at, updated_at, num_people,
                       allocated_revenue_cent, entitlement_kind, agreement_price_cent, consumption_window, exclusive_merchant_id,
                       parent_ticket_id, slot_index, bar_id
                  FROM issued_entitlements
                """;
    }

    static String ticketInstanceSelect() {
        return """
                SELECT id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,
                       buyer_customer_id, current_holder_customer_id, status, first_used_at, first_session_key,
                       created_at, updated_at, bar_id
                  FROM ticket_instances
                """;
    }
}
