package com.complex.platform.ticketing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
        // R5 审计点②：加载票种以读取有效期天数（此前未加载，valid_to 恒为 now+1 天）。
        TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        Instant now = Instant.now();
        // R5 审计点②：有效期按票种 validity_days 计算；为空回退默认 1 天（向后兼容）。
        Instant validTo = ticketType.validityDays() != null
                ? now.plus(ticketType.validityDays(), ChronoUnit.DAYS)
                : now.plusSeconds(86_400);
        // 出票方商户，仅作为模板未配置归属时的回退（保证既有单模板票仍可用）。
        String issuerMerchantId = resolveMerchantId();
        List<IssuedEntitlementResponse> responses = new ArrayList<>();
        for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template = repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
            // R5 审计点②：权益按各自模板归属（联合通票各模板可分属不同商户/场地/点位）。
            // 模板 merchant_id 为空时回退出票方，venue/point 直接取模板（可空）。
            String merchantId = template.merchantId() != null ? template.merchantId() : issuerMerchantId;
            String venueId = template.venueId();
            String pointId = template.pointId();
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
                        venueId,
                        pointId,
                        binding.totalTimes(),
                        0,
                        binding.valueAmountCent(),
                        "active",
                        now,
                        validTo,
                        now,
                        now,
                        // R5 TODO#1：核销人数来源——票种该权益绑定(binding) 的 num_people。
                        // binding 默认 1（单人票，向后兼容）；联合票/多人票 binding 配 >1 即随之生效。
                        // 核销时 verified_people = entitlement.numPeople()（接 R5 T5）。
                        binding.numPeople()
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
