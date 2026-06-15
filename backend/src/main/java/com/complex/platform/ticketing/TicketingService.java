package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketingService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int DEFAULT_OFFLINE_LIMIT = 200;
    private static final String CURRENCY_CNY = "CNY";
    private static final BigDecimal MIN_DISCOUNT_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_DISCOUNT_RATE = BigDecimal.valueOf(100);
    private static final Set<String> PRICE_VARIANTS = Set.of("group", "new_customer", "student", "channel", "member");
    private static final Set<String> PRICE_MODES = Set.of("derive_from_calendar_price", "fixed_price");
    private static final Set<String> SALES_CHANNELS = Set.of("admin", "kiosk", "miniapp", "app", "web", "channel");
    private static final Set<String> DYNAMIC_ADJUSTMENT_TYPES = Set.of("percent_delta", "fixed_delta");

    private final TicketingRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public TicketingService(
            TicketingRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<TicketTypeRecord> listTicketTypes() {
        return readPage(
                "ticket_product.viewed",
                "ticket_product.view_failed",
                "ticket.product.view",
                "ticket_type",
                repository::listTicketTypes
        );
    }

    public PageResponse<TicketPriceRuleRecord> listTicketPriceRules() {
        return readPage(
                "ticket_price_variant.viewed",
                "ticket_price_variant.view_failed",
                "ticket.price_variant.view",
                "ticket_price_rule",
                repository::listPriceRules
        );
    }

    public PageResponse<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
        return readPage(
                "ticket_dynamic_pricing.viewed",
                "ticket_dynamic_pricing.view_failed",
                "ticket.dynamic_pricing.view",
                "ticket_dynamic_pricing_rule",
                repository::listDynamicPricingRules
        );
    }

    @Transactional
    public TicketDynamicPricingRuleRecord createDynamicPricingRule(TicketDynamicPricingRuleRequest request) {
        return executeWrite(
                "createTicketDynamicPricingRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                "ticket_dynamic_pricing.changed",
                "ticket_dynamic_pricing.change_failed",
                "ticket.dynamic_pricing.manage",
                "ticket_dynamic_pricing_rule",
                () -> createDynamicPricingRuleInternal(request)
        );
    }

    @Transactional
    public TicketDynamicPricingRuleRecord publishDynamicPricingRule(String ruleId, StatusChangeRequest request) {
        String id = requireText(ruleId, "dynamic_pricing_rule_id");
        return executeWrite(
                "publishTicketDynamicPricingRule:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                "ticket_dynamic_pricing.published",
                "ticket_dynamic_pricing.publish_failed",
                "ticket.dynamic_pricing.publish",
                "ticket_dynamic_pricing_rule",
                () -> publishDynamicPricingRuleInternal(id, request)
        );
    }

    @Transactional
    public TicketDynamicPricingSimulationResponse simulateDynamicPricing(TicketDynamicPricingSimulationRequest request) {
        return executeWrite(
                "simulateTicketDynamicPricing",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingSimulationResponse.class,
                "ticket_dynamic_pricing.simulated",
                "ticket_dynamic_pricing.simulate_failed",
                "ticket.dynamic_pricing.simulate",
                "ticket_dynamic_pricing_simulation",
                () -> simulateDynamicPricingInternal(request)
        );
    }

    @Transactional
    public TicketTypeRecord createTicketType(TicketTypeRequest request) {
        return executeWrite(
                "createTicketType",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                "ticket_product.changed",
                "ticket_product.change_failed",
                "ticket.product.manage",
                "ticket_type",
                () -> {
                    Instant now = Instant.now();
                    TicketTypeRecord ticketType = new TicketTypeRecord(
                            "ticket_type_" + slug(request.idempotencyKey()),
                            requireText(request.ticketName(), "ticket_name"),
                            requireText(request.baseCalendarPriceMode(), "base_calendar_price_mode"),
                            request.weekdayPriceCent(),
                            request.weekendPriceCent(),
                            request.holidayPriceCent(),
                            request.stockRequired() != null && request.stockRequired(),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    );
                    return repository.saveTicketType(ticketType);
                }
        );
    }

    @Transactional
    public TicketPriceRuleRecord createTicketPriceRule(TicketPriceRuleRequest request) {
        return executeWrite(
                "createTicketPriceRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                "ticket_price_variant.changed",
                "ticket_price_variant.change_failed",
                "ticket.price_variant.manage",
                "ticket_price_rule",
                () -> createPriceRule(request)
        );
    }

    public PageResponse<EntitlementTemplateRecord> listEntitlementTemplates() {
        return readPage(
                "ticket_entitlement_template.viewed",
                "ticket_entitlement_template.viewed_failed",
                "ticket.entitlement.view",
                "entitlement_template",
                repository::listEntitlementTemplates
        );
    }

    @Transactional
    public EntitlementTemplateRecord createEntitlementTemplate(EntitlementTemplateRequest request) {
        return executeWrite(
                "createEntitlementTemplate",
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                "ticket_entitlement_template.changed",
                "ticket_entitlement_template.changed_failed",
                "ticket.entitlement.manage",
                "entitlement_template",
                () -> {
                    Instant now = Instant.now();
                    EntitlementTemplateRecord template = new EntitlementTemplateRecord(
                            "tpl_" + slug(request.idempotencyKey()),
                            requireText(request.name(), "name"),
                            requireText(request.entitlementType(), "entitlement_type"),
                            positiveOrDefault(request.defaultTotalTimes(), 1, "default_total_times"),
                            nonNegativeOrDefault(request.defaultValueCent(), 0L, "default_value_cent"),
                            trimToDefault(request.status(), "active"),
                            now,
                            now
                    );
                    return repository.saveEntitlementTemplate(template);
                }
        );
    }

    @Transactional
    public TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        return executeWrite(
                "bindTicketTypeEntitlement:" + requireText(ticketTypeId, "ticket_type_id"),
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeEntitlementRecord.class,
                "ticket_product_entitlement.bound",
                "ticket_product_entitlement.bound_failed",
                "ticket.product.manage",
                "ticket_type_entitlement",
                () -> {
                    TicketTypeRecord ticketType = repository.findTicketTypeById(ticketTypeId)
                            .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
                    EntitlementTemplateRecord template = repository.findEntitlementTemplateById(requireText(
                                    request.entitlementTemplateId(),
                                    "entitlement_template_id"
                            ))
                            .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_TEMPLATE_NOT_FOUND", "entitlement template not found"));
                    Instant now = Instant.now();
                    TicketTypeEntitlementRecord entitlement = new TicketTypeEntitlementRecord(
                            "ticket_link_" + slug(request.idempotencyKey()),
                            ticketType.id(),
                            template.id(),
                            positiveOrDefault(request.quantity(), 1, "quantity"),
                            positiveOrDefault(request.totalTimes(), template.defaultTotalTimes(), "total_times"),
                            request.valueAmountCent() == null ? template.defaultValueCent() : request.valueAmountCent(),
                            trimToNull(request.effectiveRule()),
                            now,
                            now
                    );
                    return repository.saveTicketTypeEntitlement(entitlement);
                }
        );
    }

    @Transactional
    public IssuedTicketResponse issueTicketTypeDirectly(String ticketTypeId, TicketIssueRequest request) {
        return executeWrite(
                "issueTicketTypeDirectly:" + requireText(ticketTypeId, "ticket_type_id"),
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                "ticket_order.issued",
                "ticket_order.issued_failed",
                "ticket.order.create",
                "issued_entitlement",
                () -> issueEntitlements(ticketTypeId, request)
        );
    }

    @Transactional
    public TicketOrderRecord createTicketOrder(TicketOrderRequest request) {
        return executeWrite(
                "createTicketOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                "ticket_order.created",
                "ticket_order.create_failed",
                "ticket.order.create",
                "ticket_order",
                () -> createOrder(request)
        );
    }

    @Transactional
    public TicketPaymentIntentRecord createTicketOrderPayIntent(String orderId, PaymentIntentRequest request) {
        String pathOrderId = requireText(orderId, "order_id");
        return executeWrite(
                "createTicketOrderPayIntent:" + pathOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPaymentIntentRecord.class,
                "payment.intent_created",
                "payment.intent_failed",
                "finance.payment_intent.create",
                "ticket_payment_intent",
                () -> createPaymentIntent(pathOrderId, request)
        );
    }

    @Transactional
    public IssuedTicketResponse issueTicketOrder(String orderId, StatusChangeRequest request) {
        String pathOrderId = requireText(orderId, "order_id");
        return executeWrite(
                "issueTicketOrder:" + pathOrderId,
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                "ticket.issued",
                "ticket.issue_failed",
                "ticket.ticket.issue",
                "issued_entitlement",
                () -> issueOrder(pathOrderId, request)
        );
    }

    @Transactional
    public TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        String id = requireText(ticketTypeId, "ticket_type_id");
        return executeWrite(
                "publishTicketType:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                "ticket_price.published",
                "ticket_price.publish_failed",
                "ticket.product.manage",
                "ticket_type",
                () -> {
                    StatusChangeRequest actual = requireRequest(request);
                    requireText(actual.reason(), "reason");
                    TicketTypeRecord current = repository.findTicketTypeById(id)
                            .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
                    Instant now = Instant.now();
                    return repository.saveTicketType(new TicketTypeRecord(
                            current.id(),
                            current.ticketName(),
                            current.baseCalendarPriceMode(),
                            current.weekdayPriceCent(),
                            current.weekendPriceCent(),
                            current.holidayPriceCent(),
                            current.stockRequired(),
                            "published",
                            current.createdAt(),
                            now
                    ));
                }
        );
    }

    public PageResponse<IssuedEntitlementRecord> listIssuedEntitlements() {
        return readPage(
                "ticket_issued_entitlement.viewed",
                "ticket_issued_entitlement.viewed_failed",
                "ticket.verify.view",
                "issued_entitlement",
                repository::listIssuedEntitlements
        );
    }

    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return repository.listTicketOrdersByCustomerId(requireText(customerId, "customer_id"));
    }

    @Transactional
    public TicketCredentialResponse getTicketCredential(String ticketId) {
        try {
            TicketCredentialResponse response = refreshCredential(ticketId, null);
            auditEventService.recordSuccess(
                    "ticket_credential.refreshed",
                    "ticket.credential.view",
                    "ticket_credential",
                    response.id(),
                    null,
                    null,
                    Map.of("ticket_id", response.ticketId())
            );
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "ticket_credential.refresh_failed",
                    "ticket.credential.view",
                    "ticket_credential",
                    ticketId,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public TicketCredentialResponse getCustomerTicketCredential(String ticketId, String customerId) {
        try {
            TicketCredentialResponse response = refreshCredential(ticketId, requireText(customerId, "customer_id"));
            auditEventService.recordSuccess(
                    "ticket_credential.refreshed",
                    "not_applicable",
                    "ticket_credential",
                    response.id(),
                    null,
                    null,
                    Map.of("ticket_id", response.ticketId(), "customer_id", customerId)
            );
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "ticket_credential.refresh_failed",
                    "not_applicable",
                    "ticket_credential",
                    ticketId,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public TicketRefundRecord requestTicketRefund(TicketRefundRequest request) {
        return executeWrite(
                "requestTicketRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                "ticket_refund.requested",
                "ticket_refund.request_failed",
                "ticket.refund.request",
                "ticket_refund",
                () -> requestRefund(request)
        );
    }

    @Transactional
    public TicketRefundRecord approveTicketRefund(String refundId, StatusChangeRequest request) {
        String id = requireText(refundId, "refund_id");
        return executeWrite(
                "approveTicketRefund:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                "ticket_refund.approved",
                "ticket_refund.approve_failed",
                "ticket.refund.approve",
                "ticket_refund",
                () -> approveRefund(id, request)
        );
    }

    public PageResponse<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
        String id = requireText(refundId, "refund_id");
        return readPage(
                "ticket_refund_line.viewed",
                "ticket_refund_line.view_failed",
                "ticket.refund.view",
                "ticket_refund_line",
                () -> repository.listTicketRefundLines(id)
        );
    }

    public OfflinePackageResponse getOfflineEntitlements(String merchantId, String venueId, int limit) {
        try {
            List<OfflineEntitlementResponse> entitlements = repository.listOfflineEntitlements(
                    trimToNull(merchantId),
                    trimToNull(venueId),
                    limit <= 0 ? DEFAULT_OFFLINE_LIMIT : limit
            );
            auditEventService.recordSuccess(
                    "terminal.offline_entitlements.viewed",
                    "terminal.device.bind",
                    "offline_entitlement",
                    "all",
                    null,
                    null,
                    Map.of("count", entitlements.size())
            );
            Instant now = Instant.now();
            return new OfflinePackageResponse(
                    now.toString(),
                    now.plusSeconds(3600).toString(),
                    entitlements
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal.offline_entitlements.viewed_failed",
                    "terminal.device.bind",
                    "offline_entitlement",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public VerificationPreviewResponse previewVerification(VerificationRequest request) {
        return executeWrite(
                "previewTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationPreviewResponse.class,
                "verification.previewed",
                "verification.failed",
                "verification.execute",
                "ticket_verification",
                () -> {
                    IssuedEntitlementRecord entitlement = entitlementByCredential(request);
                    boolean allowed = entitlement.remainingTimes() > 0 && "active".equals(entitlement.status());
                    return new VerificationPreviewResponse(
                            allowed,
                            allowed ? "ok" : "not_allowed",
                            toOfflineEntitlementResponse(entitlement)
                    );
                }
        );
    }

    @Transactional
    public VerificationResponse createVerification(VerificationRequest request) {
        return executeWrite(
                "createTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                "verification.succeeded",
                "verification.failed",
                "verification.execute",
                "ticket_verification",
                () -> verifyEntitlement(request, "android_terminal_online", null)
        );
    }

    public PageResponse<VerificationRecord> listVerifications() {
        return readPage(
                "verification.viewed",
                "verification.view_failed",
                "verification.view",
                "ticket_verification",
                repository::listVerifications
        );
    }

    @Transactional
    public SyncTasksResponse syncTerminalTasks(TerminalSyncTasksRequest request) {
        return executeWrite(
                "syncTerminalTasks",
                request == null ? null : request.idempotencyKey(),
                request,
                SyncTasksResponse.class,
                "terminal.sync_task.accepted",
                "terminal.sync_task.accepted_failed",
                "terminal.device.bind",
                "terminal_sync_task",
                () -> {
                    String deviceCode = requireText(request.deviceCode(), "device_code");
                    List<TerminalSyncTaskItemRequest> tasks = request.tasks() == null ? List.of() : request.tasks();
                    if (tasks.isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "tasks is required");
                    }
                    List<SyncTaskResult> results = new ArrayList<>();
                    for (TerminalSyncTaskItemRequest task : tasks) {
                        results.add(syncTask(deviceCode, trimToNull(request.staffId()), task));
                    }
                    return new SyncTasksResponse(Instant.now().toString(), results);
                }
        );
    }

    public static OfflineEntitlementResponse toOfflineEntitlementResponse(IssuedEntitlementRecord entitlement) {
        return new OfflineEntitlementResponse(
                entitlement.id(),
                entitlement.entitlementCode(),
                entitlement.entitlementType(),
                entitlement.merchantId(),
                entitlement.venueId(),
                entitlement.pointId(),
                entitlement.totalTimes(),
                entitlement.usedTimes(),
                entitlement.valueAmountCent(),
                entitlement.status(),
                entitlement.validFrom().toString(),
                entitlement.validTo().toString(),
                entitlement.updatedAt().toString()
        );
    }

    private TicketDynamicPricingRuleRecord createDynamicPricingRuleInternal(TicketDynamicPricingRuleRequest request) {
        TicketDynamicPricingRuleRequest actual = requireRequest(request);
        String ticketTypeId = requireText(actual.ticketTypeId(), "ticket_type_id");
        repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        LocalDate effectiveFrom = actual.effectiveFrom() == null ? LocalDate.now() : actual.effectiveFrom();
        LocalDate effectiveTo = actual.effectiveTo() == null ? effectiveFrom : actual.effectiveTo();
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "effective_to must be on or after effective_from");
        }
        String adjustmentType = requireEnum(actual.adjustmentType(), DYNAMIC_ADJUSTMENT_TYPES, "adjustment_type");
        if ("percent_delta".equals(adjustmentType) && actual.adjustmentRate() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "adjustment_rate is required");
        }
        if ("fixed_delta".equals(adjustmentType) && actual.adjustmentAmountCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "adjustment_amount_cent is required");
        }
        validateNonNegative(actual.minPriceCent(), "min_price_cent");
        validateNonNegative(actual.maxPriceCent(), "max_price_cent");
        if (actual.minPriceCent() != null && actual.maxPriceCent() != null && actual.minPriceCent() > actual.maxPriceCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "min_price_cent cannot exceed max_price_cent");
        }
        Instant now = Instant.now();
        return repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                "dynamic_price_rule_" + slug(actual.idempotencyKey()),
                ticketTypeId,
                requireText(actual.ruleName(), "rule_name"),
                actual.priority() == null ? 0 : actual.priority(),
                effectiveFrom,
                effectiveTo,
                trimToNull(actual.salesChannel()),
                adjustmentType,
                actual.adjustmentRate(),
                actual.adjustmentAmountCent(),
                actual.minPriceCent(),
                actual.maxPriceCent(),
                "draft",
                actual.conditionRule() == null ? Map.of() : Map.copyOf(actual.conditionRule()),
                trimToNull(actual.operatorNote()),
                evidence(actual.evidenceAttachmentIds()),
                now,
                now
        ));
    }

    private TicketDynamicPricingRuleRecord publishDynamicPricingRuleInternal(String ruleId, StatusChangeRequest request) {
        StatusChangeRequest actual = requireRequest(request);
        requireText(actual.reason(), "reason");
        TicketDynamicPricingRuleRecord existing = repository.findDynamicPricingRuleById(ruleId)
                .orElseThrow(() -> ApiException.notFound("DYNAMIC_PRICING_RULE_NOT_FOUND", "dynamic pricing rule not found"));
        Instant now = Instant.now();
        return repository.saveDynamicPricingRule(new TicketDynamicPricingRuleRecord(
                existing.id(),
                existing.ticketTypeId(),
                existing.ruleName(),
                existing.priority(),
                existing.effectiveFrom(),
                existing.effectiveTo(),
                existing.salesChannel(),
                existing.adjustmentType(),
                existing.adjustmentRate(),
                existing.adjustmentAmountCent(),
                existing.minPriceCent(),
                existing.maxPriceCent(),
                "published",
                existing.conditionRule(),
                trimToDefault(existing.operatorNote(), actual.reason()),
                evidenceOrExisting(actual.evidenceAttachmentIds(), existing.evidenceAttachmentIds()),
                existing.createdAt(),
                now
        ));
    }

    private TicketDynamicPricingSimulationResponse simulateDynamicPricingInternal(TicketDynamicPricingSimulationRequest request) {
        TicketDynamicPricingSimulationRequest actual = requireRequest(request);
        TicketTypeRecord ticketType = repository.findTicketTypeById(requireText(actual.ticketTypeId(), "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        LocalDate visitDate = actual.visitDate() == null ? LocalDate.now() : actual.visitDate();
        String salesChannel = requireEnum(actual.salesChannel(), SALES_CHANNELS, "sales_channel");
        long basePrice = resolveUnitPrice(ticketType, null, visitDate);
        Map<String, Object> context = actual.context() == null ? Map.of() : actual.context();
        TicketDynamicPricingRuleRecord matched = repository.listDynamicPricingRules().stream()
                .filter(rule -> ticketType.id().equals(rule.ticketTypeId()))
                .filter(rule -> "draft".equals(rule.status()) || "published".equals(rule.status()))
                .filter(rule -> !visitDate.isBefore(rule.effectiveFrom()) && !visitDate.isAfter(rule.effectiveTo()))
                .filter(rule -> rule.salesChannel() == null || rule.salesChannel().equals(salesChannel))
                .filter(rule -> matchesCondition(rule.conditionRule(), context))
                .sorted((left, right) -> {
                    int priority = Integer.compare(right.priority(), left.priority());
                    return priority != 0 ? priority : right.updatedAt().compareTo(left.updatedAt());
                })
                .findFirst()
                .orElse(null);
        if (matched == null) {
            return new TicketDynamicPricingSimulationResponse(
                    ticketType.id(),
                    visitDate,
                    salesChannel,
                    basePrice,
                    0L,
                    basePrice,
                    null,
                    "no matching dynamic pricing rule"
            );
        }
        long rawAdjustment = dynamicAdjustment(basePrice, matched);
        long finalPrice = clampPrice(basePrice + rawAdjustment, matched.minPriceCent(), matched.maxPriceCent());
        return new TicketDynamicPricingSimulationResponse(
                ticketType.id(),
                visitDate,
                salesChannel,
                basePrice,
                finalPrice - basePrice,
                finalPrice,
                matched.id(),
                "matched " + matched.ruleName()
        );
    }

    private TicketPriceRuleRecord createPriceRule(TicketPriceRuleRequest request) {
        TicketPriceRuleRequest actual = requireRequest(request);
        String ticketTypeId = requireText(actual.ticketTypeId(), "ticket_type_id");
        repository.findTicketTypeById(ticketTypeId)
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "ticket type not found"));
        String variantType = requireEnum(actual.variantType(), PRICE_VARIANTS, "variant_type");
        String priceMode = requireEnum(actual.priceMode(), PRICE_MODES, "price_mode");
        if ("fixed_price".equals(priceMode)
                && actual.weekdayPriceCent() == null
                && actual.weekendPriceCent() == null
                && actual.holidayPriceCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "fixed price requires at least one price");
        }
        if ("derive_from_calendar_price".equals(priceMode)
                && (actual.discountRate() == null
                || actual.discountRate().compareTo(MIN_DISCOUNT_RATE) < 0
                || actual.discountRate().compareTo(MAX_DISCOUNT_RATE) > 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "discount_rate must be between 0 and 100");
        }
        validateNonNegative(actual.weekdayPriceCent(), "weekday_price_cent");
        validateNonNegative(actual.weekendPriceCent(), "weekend_price_cent");
        validateNonNegative(actual.holidayPriceCent(), "holiday_price_cent");
        Instant now = Instant.now();
        return repository.savePriceRule(new TicketPriceRuleRecord(
                "price_rule_" + slug(actual.idempotencyKey()),
                ticketTypeId,
                variantType,
                priceMode,
                actual.weekdayPriceCent(),
                actual.weekendPriceCent(),
                actual.holidayPriceCent(),
                actual.discountRate(),
                trimToNull(actual.channelId()),
                CURRENCY_CNY,
                "active",
                actual.eligibilityRule() == null ? Map.of() : Map.copyOf(actual.eligibilityRule()),
                now,
                now
        ));
    }

    private TicketOrderRecord createOrder(TicketOrderRequest request) {
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
        long unitPrice = resolveUnitPrice(ticketType, priceRule, visitDate);
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
                unitPrice * quantity,
                CURRENCY_CNY,
                "unpaid",
                "not_issued",
                "created",
                now,
                now
        ));
    }

    private TicketPaymentIntentRecord createPaymentIntent(String orderId, PaymentIntentRequest request) {
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

    private IssuedTicketResponse issueOrder(String orderId, StatusChangeRequest request) {
        requireRequest(request);
        requireText(request.reason(), "reason");
        TicketOrderRecord order = repository.findTicketOrderById(orderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        List<IssuedEntitlementRecord> existing = repository.listIssuedEntitlementsByOrderId(orderId);
        if (!existing.isEmpty()) {
            return toIssuedTicketResponse(existing);
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
                order.currency(),
                "paid",
                "issued",
                order.status(),
                order.createdAt(),
                Instant.now()
        ));
        return issued;
    }

    private TicketCredentialResponse refreshCredential(String ticketId, String customerId) {
        IssuedEntitlementRecord entitlement = repository.findIssuedEntitlementById(requireText(ticketId, "ticket_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        if (customerId != null) {
            TicketOrderRecord order = repository.findTicketOrderById(entitlement.orderId())
                    .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
            if (!customerId.equals(order.customerId())) {
                throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "ticket does not belong to customer");
            }
        }
        Instant now = Instant.now();
        String token = "cred_" + UUID.randomUUID().toString().replace("-", "");
        TicketCredentialRecord saved = repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_" + UUID.randomUUID().toString().replace("-", ""),
                entitlement.id(),
                entitlement.orderId(),
                token,
                UUID.randomUUID().toString().replace("-", ""),
                hash(token),
                "dynamic_qr",
                now,
                now.plusSeconds(300),
                "active",
                1,
                now,
                now
        ));
        return toCredentialResponse(saved);
    }

    private TicketRefundRecord requestRefund(TicketRefundRequest request) {
        TicketRefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        TicketOrderRecord order = repository.findTicketOrderById(targetId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        long amountCent = requirePositiveLong(actual.refundAmountCent(), "refund_amount_cent");
        if (amountCent > order.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount exceeds order amount");
        }
        Instant now = Instant.now();
        TicketRefundRecord refund = repository.saveTicketRefund(new TicketRefundRecord(
                "ticket_refund_" + slug(actual.idempotencyKey()),
                targetId,
                amountCent,
                requireText(actual.reason(), "reason"),
                trimToDefault(actual.refundChannel(), "original"),
                "requested",
                null,
                null,
                now,
                now
        ));
        createRefundLines(refund, order, now);
        return refund;
    }

    private TicketRefundRecord approveRefund(String refundId, StatusChangeRequest request) {
        requireRequest(request);
        requireText(request.reason(), "reason");
        TicketRefundRecord current = repository.findTicketRefundById(refundId)
                .orElseThrow(() -> ApiException.notFound("TICKET_REFUND_NOT_FOUND", "ticket refund not found"));
        Instant now = Instant.now();
        TicketRefundRecord approved = repository.saveTicketRefund(new TicketRefundRecord(
                current.id(),
                current.targetId(),
                current.refundAmountCent(),
                current.reason(),
                current.refundChannel(),
                "approved",
                "staff",
                now,
                current.createdAt(),
                now
        ));
        repository.listTicketRefundLines(current.id()).forEach(line -> repository.saveTicketRefundLine(new TicketRefundLineRecord(
                line.id(),
                line.refundId(),
                line.orderId(),
                line.entitlementId(),
                line.entitlementType(),
                line.lineType(),
                line.refundAmountCent(),
                line.remainingTimesBefore(),
                line.usedTimesBefore(),
                "approved",
                line.createdAt(),
                now
        )));
        return approved;
    }

    private void createRefundLines(TicketRefundRecord refund, TicketOrderRecord order, Instant now) {
        List<IssuedEntitlementRecord> entitlements = repository.listIssuedEntitlementsByOrderId(order.id());
        if (entitlements.isEmpty()) {
                    repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    refundLineId(refund.id(), 0),
                    refund.id(),
                    order.id(),
                    null,
                    null,
                    "order",
                    refund.refundAmountCent(),
                    0,
                    0,
                    "requested",
                    now,
                    now
            ));
            return;
        }

        long remaining = refund.refundAmountCent();
        for (int index = 0; index < entitlements.size() && remaining > 0; index++) {
            IssuedEntitlementRecord entitlement = entitlements.get(index);
            boolean lastLine = index == entitlements.size() - 1;
            long suggestedAmount = entitlement.valueAmountCent() == null || entitlement.valueAmountCent() <= 0
                    ? Math.max(1L, order.amountCent() / Math.max(1, entitlements.size()))
                    : entitlement.valueAmountCent();
            long lineAmount = lastLine ? remaining : Math.min(suggestedAmount, remaining);
            if (lineAmount <= 0) {
                continue;
            }
            remaining -= lineAmount;
            repository.saveTicketRefundLine(new TicketRefundLineRecord(
                    refundLineId(refund.id(), index),
                    refund.id(),
                    order.id(),
                    entitlement.id(),
                    entitlement.entitlementType(),
                    "entitlement",
                    lineAmount,
                    entitlement.remainingTimes(),
                    entitlement.usedTimes(),
                    "requested",
                    now,
                    now
            ));
        }
        if (remaining > 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "refund amount cannot be allocated to entitlement lines");
        }
    }

    private String refundLineId(String refundId, int index) {
        return "refund_line_" + slug(refundId) + "_" + index;
    }

    private IssuedTicketResponse issueEntitlements(String ticketTypeId, TicketIssueRequest request) {
        String orderId = requireText(request.orderId(), "order_id");
        List<TicketTypeEntitlementRecord> bindings = repository.listTicketTypeEntitlements(ticketTypeId);
        if (bindings.isEmpty()) {
            throw ApiException.conflict("TICKET_TYPE_ENTITLEMENT_REQUIRED", "ticket type has no entitlement templates");
        }
        Instant now = Instant.now();
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
                        null,
                        null,
                        null,
                        binding.totalTimes(),
                        0,
                        binding.valueAmountCent(),
                        "active",
                        now,
                        now.plusSeconds(86_400),
                        now,
                        now
                );
                repository.saveIssuedEntitlement(entitlement);
                responses.add(new IssuedEntitlementResponse(entitlementCode));
            }
        }
        return new IssuedTicketResponse(responses);
    }

    private VerificationResponse verifyEntitlement(VerificationRequest request, String sourceType, String clientTaskId) {
        TicketCredentialRecord credential = credentialByPayload(request == null ? null : request.credentialPayload()).orElse(null);
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE", "entitlement is not active");
        }
        if (entitlement.remainingTimes() <= 0) {
            throw ApiException.conflict("ENTITLEMENT_USED_UP", "entitlement has no remaining times");
        }

        Instant now = Instant.now();
        IssuedEntitlementRecord updated = repository.updateIssuedEntitlement(entitlement.useOnce(now));
        VerificationRecord verification = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                updated.id(),
                updated.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                requireText(request.credentialPayload(), "credential_payload"),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "success",
                null,
                sourceType,
                clientTaskId,
                now
        ));
        if (credential != null) {
            repository.saveTicketCredential(new TicketCredentialRecord(
                    credential.id(),
                    credential.entitlementId(),
                    credential.orderId(),
                    credential.credentialToken(),
                    credential.nonce(),
                    credential.credentialHash(),
                    credential.credentialType(),
                    credential.issuedAt(),
                    credential.expiresAt(),
                    "consumed",
                    credential.refreshedCount(),
                    credential.createdAt(),
                    now
            ));
        }
        return new VerificationResponse(verification.id(), verification.verificationNo(), verification.status(), verification.resultReason());
    }

    private SyncTaskResult syncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        return repository.findTerminalSyncTask(deviceCode, clientTaskId)
                .map(this::toSyncTaskResult)
                .orElseGet(() -> createSyncTask(deviceCode, staffId, task));
    }

    private SyncTaskResult createSyncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String taskType = requireText(task.taskType(), "task_type");
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        String occurredAt = requireText(task.occurredAt(), "occurred_at");
        Map<String, String> payload = task.payload() == null ? Map.of() : task.payload();
        String verificationId = null;
        String verificationNo = null;

        if ("offline_verification".equals(taskType)) {
            String entitlementCode = firstPayload(payload, "entitlementCode", "entitlement_code", "credentialPayload", "credential_payload");
            String pointId = firstPayloadOptional(payload, "pointId", "point_id", "gateId", "gate_id", "point");
            VerificationResponse verification = verifyEntitlement(
                    new VerificationRequest(
                            "sync-task-" + clientTaskId,
                            "qr",
                            entitlementCode,
                            deviceCode,
                            pointId == null ? deviceCode : pointId,
                            Map.of("source", "terminal_sync_task")
                    ),
                    "android_terminal_offline",
                    clientTaskId
            );
            verificationId = verification.id();
            verificationNo = verification.verificationNo();
        }

        TerminalSyncTaskRecord saved = repository.saveTerminalSyncTask(new TerminalSyncTaskRecord(
                "sync_" + UUID.randomUUID().toString().replace("-", ""),
                deviceCode,
                staffId,
                clientTaskId,
                taskType,
                "accepted",
                null,
                verificationId,
                verificationNo,
                occurredAt,
                payload,
                Instant.now()
        ));
        return toSyncTaskResult(saved);
    }

    private SyncTaskResult toSyncTaskResult(TerminalSyncTaskRecord task) {
        return new SyncTaskResult(
                task.clientTaskId(),
                task.status(),
                task.reason(),
                task.verificationId(),
                task.verificationNo(),
                null
        );
    }

    private IssuedEntitlementRecord entitlementByCredential(VerificationRequest request) {
        String entitlementCode = requireText(request == null ? null : request.credentialPayload(), "credential_payload");
        if (entitlementCode.startsWith("cred_")) {
            TicketCredentialRecord credential = credentialByPayload(entitlementCode)
                    .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
            return repository.findIssuedEntitlementById(credential.entitlementId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        }
        return repository.findIssuedEntitlementByCode(entitlementCode)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
    }

    private java.util.Optional<TicketCredentialRecord> credentialByPayload(String credentialPayload) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (!payload.startsWith("cred_")) {
            return java.util.Optional.empty();
        }
        TicketCredentialRecord credential = repository.findTicketCredentialByToken(payload)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
        if (!"active".equals(credential.status()) || credential.expiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "credential is expired");
        }
        return java.util.Optional.of(credential);
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        try {
            List<T> items = action.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T saved = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(saved),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return saved;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private long requirePositiveLong(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private void validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = requireText(value, fieldName);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        int resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return resolved;
    }

    private long nonNegativeOrDefault(Long value, long defaultValue, String fieldName) {
        long resolved = value == null ? defaultValue : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    private String firstPayload(Map<String, String> payload, String... keys) {
        String value = firstPayloadOptional(payload, keys);
        if (value != null) {
            return value;
        }
        throw ApiException.badRequest("VALIDATION_FAILED", String.join("/", keys) + " is required");
    }

    private String firstPayloadOptional(Map<String, String> payload, String... keys) {
        for (String key : keys) {
            String value = payload.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> evidence(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> evidenceOrExisting(List<String> values, List<String> existing) {
        List<String> evidence = evidence(values);
        return evidence.isEmpty() ? List.copyOf(existing == null ? List.of() : existing) : evidence;
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

    private boolean matchesCondition(Map<String, Object> conditionRule, Map<String, Object> context) {
        if (conditionRule == null || conditionRule.isEmpty()) {
            return true;
        }
        return conditionRule.entrySet().stream()
                .allMatch(entry -> {
                    Object actual = context.get(entry.getKey());
                    return actual != null && String.valueOf(actual).equals(String.valueOf(entry.getValue()));
                });
    }

    private long dynamicAdjustment(long basePrice, TicketDynamicPricingRuleRecord rule) {
        if ("fixed_delta".equals(rule.adjustmentType())) {
            return rule.adjustmentAmountCent() == null ? 0L : rule.adjustmentAmountCent();
        }
        BigDecimal rate = rule.adjustmentRate() == null ? BigDecimal.ZERO : rule.adjustmentRate();
        return BigDecimal.valueOf(basePrice)
                .multiply(rate)
                .divide(MAX_DISCOUNT_RATE, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private long clampPrice(long value, Long minPriceCent, Long maxPriceCent) {
        long clamped = minPriceCent == null ? value : Math.max(value, minPriceCent);
        return maxPriceCent == null ? clamped : Math.min(clamped, maxPriceCent);
    }

    private long resolveUnitPrice(TicketTypeRecord ticketType, TicketPriceRuleRecord priceRule, LocalDate visitDate) {
        Long base = switch (visitDate.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> firstNonNull(ticketType.weekendPriceCent(), ticketType.weekdayPriceCent(), ticketType.holidayPriceCent());
            default -> firstNonNull(ticketType.weekdayPriceCent(), ticketType.weekendPriceCent(), ticketType.holidayPriceCent());
        };
        if (base == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "ticket type base price is required");
        }
        if (priceRule == null) {
            return base;
        }
        Long fixed = switch (visitDate.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> firstNonNull(priceRule.weekendPriceCent(), priceRule.weekdayPriceCent(), priceRule.holidayPriceCent());
            default -> firstNonNull(priceRule.weekdayPriceCent(), priceRule.weekendPriceCent(), priceRule.holidayPriceCent());
        };
        if ("fixed_price".equals(priceRule.priceMode())) {
            if (fixed == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "fixed price is required");
            }
            return fixed;
        }
        BigDecimal discountRate = priceRule.discountRate() == null ? MAX_DISCOUNT_RATE : priceRule.discountRate();
        return BigDecimal.valueOf(base)
                .multiply(discountRate)
                .divide(MAX_DISCOUNT_RATE, 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private Long firstNonNull(Long first, Long second, Long third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private IssuedTicketResponse toIssuedTicketResponse(List<IssuedEntitlementRecord> entitlements) {
        return new IssuedTicketResponse(entitlements.stream()
                .map(entitlement -> new IssuedEntitlementResponse(entitlement.entitlementCode()))
                .toList());
    }

    private TicketCredentialResponse toCredentialResponse(TicketCredentialRecord credential) {
        return new TicketCredentialResponse(
                credential.id(),
                credential.entitlementId(),
                credential.orderId(),
                credential.credentialToken(),
                credential.credentialType(),
                credential.issuedAt(),
                credential.expiresAt(),
                credential.refreshedCount()
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("credential hashing unavailable", exception);
        }
    }

    private String businessId(Object value) {
        if (value instanceof TicketTypeRecord record) {
            return record.id();
        }
        if (value instanceof EntitlementTemplateRecord record) {
            return record.id();
        }
        if (value instanceof TicketTypeEntitlementRecord record) {
            return record.id();
        }
        if (value instanceof VerificationResponse response) {
            return response.id();
        }
        if (value instanceof IssuedTicketResponse response && !response.issuedEntitlements().isEmpty()) {
            return response.issuedEntitlements().getFirst().entitlementCode();
        }
        if (value instanceof SyncTasksResponse response && !response.results().isEmpty()) {
            return response.results().getFirst().clientTaskId();
        }
        if (value instanceof TicketPriceRuleRecord record) {
            return record.id();
        }
        if (value instanceof TicketOrderRecord record) {
            return record.id();
        }
        if (value instanceof TicketPaymentIntentRecord record) {
            return record.id();
        }
        if (value instanceof TicketRefundRecord record) {
            return record.id();
        }
        return null;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
