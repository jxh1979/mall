package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TicketOrderService {

    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel");

    private final TicketingRepository repository;
    private final TicketPricingService pricingService;
    private final AuthorizationService authorizationService;

    public TicketOrderService(TicketingRepository repository, TicketPricingService pricingService,
                              AuthorizationService authorizationService) {
        this.repository = repository;
        this.pricingService = pricingService;
        this.authorizationService = authorizationService;
    }

    TicketOrderRecord createOrder(TicketOrderRequest request) {
        TicketOrderRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(actual.ticketTypeId(), "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        int quantity = requirePositive(actual.quantity(), "quantity");
        LocalDate visitDate = actual.visitDate() == null ? LocalDate.now() : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        TicketPriceRuleRecord priceRule = null;
        if (trimToNull(actual.priceRuleId()) != null) {
            priceRule = repository.findPriceRuleById(actual.priceRuleId())
                    .orElseThrow(() -> ApiException.notFound("PRICE_RULE_NOT_FOUND", "price rule not found"));
            if (!ticketType.id().equals(priceRule.ticketTypeId())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "price_rule_id does not match ticket_type_id");
            }
        }
        long unitPrice = pricingService.resolveUnitPrice(ticketType, priceRule, visitDate);
        DynamicPriceResult dynamicPrice = pricingService.applyPublishedDynamicPricing(
                ticketType.id(),
                visitDate,
                salesChannel,
                unitPrice,
                actual.dynamicPricingContext()
        );
        long baseAmount = unitPrice * quantity;
        long dynamicAdjustment = dynamicPrice.adjustmentCent() * quantity;
        long finalAmount = dynamicPrice.finalUnitPriceCent() * quantity;
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        return repository.saveTicketOrder(new TicketOrderRecord(
                "ticket_order_" + slug,
                "TO" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                trimToNull(actual.customerId()),
                ticketType.id(),
                priceRule == null ? null : priceRule.id(),
                quantity,
                visitDate,
                salesChannel,
                trimToNull(actual.buyerPhone()),
                finalAmount,
                dynamicPrice.ruleId(),
                baseAmount,
                dynamicAdjustment,
                CURRENCY_CNY,
                "unpaid",
                "not_issued",
                "created",
                now,
                now
        ));
    }

    TicketPaymentIntentRecord createPaymentIntent(String orderId, PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        if (!orderId.equals(businessOrderId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "business_order_id must match path order_id");
        }
        if (!"ticket_order".equals(requireText(actual.businessOrderType(), "business_order_type"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "business_order_type must be ticket_order");
        }
        long amountCent = requirePositiveLong(actual.amountCent(), "amount_cent");
        if (amountCent != order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must equal ticket order amount");
        }
        String currency = trimToDefault(actual.currency(), CURRENCY_CNY);
        if (!CURRENCY_CNY.equals(currency)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "currency must be CNY");
        }
        Instant now = Instant.now();
        return repository.saveTicketPaymentIntent(new TicketPaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                "ticket_order",
                orderId,
                amountCent,
                CURRENCY_CNY,
                requireText(actual.channel(), "channel"),
                "created",
                now,
                now
        ));
    }

    IssuedTicketResponse issueOrder(String orderId, StatusChangeRequest request) {
        requireRequest(request);
        requireText(request.reason(), "reason");
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        List<IssuedEntitlementRecord> existing = repository.listIssuedEntitlementsByOrderId(orderId);
        if (!existing.isEmpty()) {
            return toIssuedTicketResponse(existing);
        }
        if (!"paid".equals(order.payStatus()) && !"admin".equals(order.salesChannel())) {
            throw ApiException.conflict("ORDER_NOT_PAID", "order must be paid before issuing");
        }
        if (!repository.tryLockOrderForIssuance(orderId)) {
            List<IssuedEntitlementRecord> recheck = repository.listIssuedEntitlementsByOrderId(orderId);
            if (!recheck.isEmpty()) {
                return toIssuedTicketResponse(recheck);
            }
            throw ApiException.conflict("ORDER_ISSUANCE_IN_PROGRESS", "order issuance already in progress");
        }
        IssuedTicketResponse issued = issueEntitlements(order.ticketTypeId(), new TicketIssueRequest(
                request.idempotencyKey(),
                request.reason(),
                request.evidenceAttachmentIds(),
                order.id(),
                order.salesChannel(),
                List.of("dynamic_qr")
        ));
        repository.updateTicketOrder(new TicketOrderRecord(
                order.id(),
                order.orderNo(),
                order.customerId(),
                order.ticketTypeId(),
                order.priceRuleId(),
                order.quantity(),
                order.visitDate(),
                order.salesChannel(),
                order.buyerPhone(),
                order.amountCent(),
                order.dynamicPricingRuleId(),
                order.baseAmountCent(),
                order.dynamicAdjustmentCent(),
                order.currency(),
                "paid",
                "issued",
                order.status(),
                order.createdAt(),
                Instant.now()
        ));
        return issued;
    }

    IssuedTicketResponse issueEntitlements(String ticketTypeId, TicketIssueRequest request) {
        String orderId = requireText(request.orderId(), "order_id");
        List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        if (bindings.isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED", "ticket type has no entitlement templates");
        }
        Instant now = Instant.now();
        String merchantId = resolveMerchantId();
        List<IssuedEntitlementResponse> responses = new ArrayList<>();
        for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template = repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
            for (int i = 0; i < binding.quantity(); i++) {
                String entitlementCode = "P0-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                IssuedEntitlementRecord entitlement = new IssuedEntitlementRecord(
                        "ent_" + UUID.randomUUID().toString().replace("-", ""),
                        entitlementCode,
                        ticketTypeId,
                        orderId,
                        template.id(),
                        template.entitlementType(),
                        merchantId,
                        null,
                        null,
                        binding.totalTimes(),
                        0,
                        binding.valueAmountCent(),
                        "active",
                        now,
                        now.plusSeconds(86_400),
                        now,
                        now,
                        // R5 T5：核销人数。票种/binding(TicketTypeEntitlementRecord) 当前无人数字段（已 grep 确认），
                        // 故 num_people 默认 1。TODO 多人票 num_people>1 来源待票种配置（后续：binding/票种加人数字段后填充）。
                        1
                );
                repository.saveIssuedEntitlement(entitlement);
                responses.add(new IssuedEntitlementResponse(entitlementCode));
            }
        }
        return new IssuedTicketResponse(responses);
    }

    private String resolveMerchantId() {
        if (authorizationService == null) {
            return null;
        }
        try {
            return authorizationService.currentMerchantScope().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private IssuedTicketResponse toIssuedTicketResponse(List<IssuedEntitlementRecord> entitlements) {
        return new IssuedTicketResponse(entitlements.stream()
                .map(entitlement -> new IssuedEntitlementResponse(entitlement.entitlementCode()))
                .toList());
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private int requirePositive(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private long requirePositiveLong(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved <= 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String actual = requireText(value, fieldName);
        if (!allowed.contains(actual)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return actual;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }
}
