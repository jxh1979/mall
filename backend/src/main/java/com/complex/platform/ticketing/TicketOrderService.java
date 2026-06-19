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
        // 待确认项#1：拆分基数=订单实付（无订单则不拆分，allocated=null）。direct 出票（issueTicketTypeDirectly）
        // 用合成 order_id 多无真实订单，此时跳过拆分，向后兼容既有单模板票。
        Long orderAmount = repository.findTicketOrderById(orderId).map(TicketOrderRecord::amountCent).orElse(null);
        long[] allocations = allocateRevenue(bindings, orderAmount);

        List<IssuedEntitlementResponse> responses = new ArrayList<>();
        int allocationIndex = 0;
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
                // 待确认项#1：每张权益分配收入（无订单则 null）。业态归属由 entitlement_type + merchant_id 在聚合侧映射。
                Long allocatedRevenue = allocations == null ? null : allocations[allocationIndex];
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
                        binding.numPeople(),
                        allocatedRevenue
                );
                repository.saveIssuedEntitlement(entitlement);
                responses.add(new IssuedEntitlementResponse(entitlementCode));
                allocationIndex++;
            }
        }
        return new IssuedTicketResponse(responses);
    }

    /**
     * 待确认项#1：把订单实付按各权益分项单价比例拆分到每张已发权益（每个 binding 发 quantity 张，逐张分配）。
     *
     * <p>拆分算法（财务确认：酒吧+游乐组合票按打折比例拆分，法律双实体；也支持手动输入各分项单价）：
     * <ul>
     *   <li>权重 = binding.componentPriceCent（分项标准单价/手动单价），每张权益按其 binding 的权重参与分配。</li>
     *   <li>allocated(每张) = orderAmount × (componentPrice / Σ(componentPrice × quantity))。</li>
     *   <li>分项单价全空 → 回退按数量均摊（orderAmount 平均分到每张权益，口径：无分项单价时各权益等额）。</li>
     *   <li>订单实付为 null（direct 出票无订单）→ 返回 null，不拆分（向后兼容）。</li>
     * </ul>
     * 采用"逐张整数分配 + 最大余数补尾"使各张之和恰等于 orderAmount（分到分，不丢分、不重复计）。
     *
     * @return 与发券顺序一致的每张权益分配收入数组；不拆分时返回 null。
     */
    private long[] allocateRevenue(List<TicketTypeEntitlementRecord> bindings, Long orderAmount) {
        if (orderAmount == null) {
            return null;
        }
        int totalUnits = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            totalUnits += binding.quantity();
        }
        if (totalUnits == 0) {
            return new long[0];
        }
        // 每张权益的权重（分项单价；全空回退均摊→权重置 1）。
        long[] weights = new long[totalUnits];
        long totalWeight = 0;
        boolean anyComponentPrice = bindings.stream().anyMatch(b -> b.componentPriceCent() != null);
        int idx = 0;
        for (TicketTypeEntitlementRecord binding : bindings) {
            // 分项单价全空回退均摊：每张权重=1；否则取本 binding 单价（缺省 0，不参与分配）。
            long unitWeight = anyComponentPrice
                    ? (binding.componentPriceCent() == null ? 0L : binding.componentPriceCent())
                    : 1L;
            for (int i = 0; i < binding.quantity(); i++) {
                weights[idx] = unitWeight;
                totalWeight += unitWeight;
                idx++;
            }
        }
        long[] allocations = new long[totalUnits];
        if (totalWeight <= 0) {
            // 有分项单价配置但全为 0（异常配置）→ 退化为按数量均摊，保证不丢分。
            return allocateEvenly(orderAmount, totalUnits);
        }
        // 逐张整数分配 + 最大余数补尾，使 Σallocations == orderAmount。
        long assigned = 0;
        double[] remainders = new double[totalUnits];
        for (int i = 0; i < totalUnits; i++) {
            long product = orderAmount * weights[i];
            long floor = product / totalWeight;
            allocations[i] = floor;
            assigned += floor;
            remainders[i] = (double) (product - floor * totalWeight) / totalWeight;
        }
        long remainder = orderAmount - assigned;
        // 把余数（最多 totalUnits-1 分）按小数余数从大到小逐分补给权重>0 的张。
        while (remainder > 0) {
            int best = -1;
            double bestRem = -1.0;
            for (int i = 0; i < totalUnits; i++) {
                if (weights[i] > 0 && remainders[i] > bestRem) {
                    bestRem = remainders[i];
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            allocations[best] += 1;
            remainders[best] = -1.0;
            remainder -= 1;
        }
        return allocations;
    }

    private long[] allocateEvenly(long orderAmount, int totalUnits) {
        long[] allocations = new long[totalUnits];
        long base = orderAmount / totalUnits;
        long remainder = orderAmount - base * totalUnits;
        for (int i = 0; i < totalUnits; i++) {
            allocations[i] = base + (i < remainder ? 1 : 0);
        }
        return allocations;
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
