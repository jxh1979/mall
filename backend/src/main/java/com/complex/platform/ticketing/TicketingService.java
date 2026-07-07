package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateArchiveRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.dto.VerificationReverseRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketingService {

    private final TicketingRepository repository;
    private final TicketCatalogService catalogService;
    private final TicketPricingService pricingService;
    private final TicketOrderService orderService;
    private final TicketRefundService refundService;
    private final TicketVerificationService verificationService;
    private final TicketCredentialService credentialService;
    private final TicketOfflinePackageService offlinePackageService;
    private final TicketingOperationExecutor operations;
    // P1 越权修复：核销/权益列表按调用者商户范围过滤（记录含 merchant_id）。可为 null=兼容既有测试构造（不过滤）。
    private final AuthorizationService authorizationService;

    public TicketingService(
            TicketingRepository repository,
            TicketCatalogService catalogService,
            TicketPricingService pricingService,
            TicketOrderService orderService,
            TicketRefundService refundService,
            TicketVerificationService verificationService,
            TicketCredentialService credentialService,
            TicketOfflinePackageService offlinePackageService,
            TicketingOperationExecutor operations,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.pricingService = pricingService;
        this.orderService = orderService;
        this.refundService = refundService;
        this.verificationService = verificationService;
        this.credentialService = credentialService;
        this.offlinePackageService = offlinePackageService;
        this.operations = operations;
        this.authorizationService = authorizationService;
    }

    /**
     * P1 越权修复：核销/权益列表按调用者商户范围过滤——商户用户仅见自身 merchant_id 数据，
     * 平台管理员/无商户范围者（empty）见全部。authorizationService 为 null（兼容测试）时不过滤。
     */
    private <T> java.util.List<T> filterByMerchantScope(
            java.util.List<T> rows, java.util.function.Function<T, String> merchantIdOf) {
        if (authorizationService == null) {
            return rows;
        }
        java.util.Optional<String> scope = authorizationService.currentMerchantIdQuietly();
        if (scope.isEmpty()) {
            return rows;
        }
        String merchantId = scope.get();
        return rows.stream().filter(row -> merchantId.equals(merchantIdOf.apply(row))).toList();
    }

    public PageResponse<TicketTypeRecord> listTicketTypes() {
        return listTicketTypes(false);
    }

    public PageResponse<TicketTypeRecord> listTicketTypes(boolean includeArchived) {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_TYPES,
                () -> repository.listTicketTypes(includeArchived)
        );
    }

    public PageResponse<TicketPriceRuleRecord> listTicketPriceRules() {
        return listTicketPriceRules(false);
    }

    public PageResponse<TicketPriceRuleRecord> listTicketPriceRules(boolean includeArchived) {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_PRICE_RULES,
                () -> repository.listPriceRules(includeArchived)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord archiveTicketType(String id, TicketCatalogArchiveRequest request) {
        String ticketTypeId = operations.requireText(id, "id");
        return operations.executeWrite(
                "archiveTicketType:" + ticketTypeId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.ARCHIVE_TICKET_TYPE,
                () -> catalogService.archiveTicketType(ticketTypeId, true, request == null ? null : request.reason(), actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord unarchiveTicketType(String id, TicketCatalogArchiveRequest request) {
        String ticketTypeId = operations.requireText(id, "id");
        return operations.executeWrite(
                "unarchiveTicketType:" + ticketTypeId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.UNARCHIVE_TICKET_TYPE,
                () -> catalogService.archiveTicketType(ticketTypeId, false, null, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord requestDeleteTicketType(String id, TicketCatalogDeleteRequest request) {
        String ticketTypeId = operations.requireText(id, "id");
        return operations.executeWrite(
                "requestDeleteTicketType:" + ticketTypeId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.REQUEST_DELETE_TICKET_TYPE,
                () -> catalogService.requestDeleteTicketType(ticketTypeId, request, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.type.approve")
    @Transactional
    public TicketTypeDeleteDecision approveDeleteTicketType(String id, TicketCatalogDeleteApprovalRequest request) {
        String ticketTypeId = operations.requireText(id, "id");
        return operations.executeWrite(
                "approveDeleteTicketType:" + ticketTypeId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeDeleteDecision.class,
                TicketingWriteOperation.APPROVE_DELETE_TICKET_TYPE,
                () -> catalogService.approveDeleteTicketType(ticketTypeId, request, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketPriceRuleRecord archivePriceRule(String id, TicketCatalogArchiveRequest request) {
        String ruleId = operations.requireText(id, "id");
        return operations.executeWrite(
                "archivePriceRule:" + ruleId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                TicketingWriteOperation.ARCHIVE_TICKET_PRICE_RULE,
                () -> catalogService.archivePriceRule(ruleId, true, request == null ? null : request.reason(), actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketPriceRuleRecord unarchivePriceRule(String id, TicketCatalogArchiveRequest request) {
        String ruleId = operations.requireText(id, "id");
        return operations.executeWrite(
                "unarchivePriceRule:" + ruleId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                TicketingWriteOperation.UNARCHIVE_TICKET_PRICE_RULE,
                () -> catalogService.archivePriceRule(ruleId, false, null, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketPriceRuleRecord requestDeletePriceRule(String id, TicketCatalogDeleteRequest request) {
        String ruleId = operations.requireText(id, "id");
        return operations.executeWrite(
                "requestDeletePriceRule:" + ruleId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                TicketingWriteOperation.REQUEST_DELETE_TICKET_PRICE_RULE,
                () -> catalogService.requestDeletePriceRule(ruleId, request, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_rule.approve")
    @Transactional
    public TicketPriceRuleDeleteDecision approveDeletePriceRule(String id, TicketCatalogDeleteApprovalRequest request) {
        String ruleId = operations.requireText(id, "id");
        return operations.executeWrite(
                "approveDeletePriceRule:" + ruleId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleDeleteDecision.class,
                TicketingWriteOperation.APPROVE_DELETE_TICKET_PRICE_RULE,
                () -> catalogService.approveDeletePriceRule(ruleId, request, actorId())
        );
    }

    public PageResponse<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
        return operations.readPage(
                TicketingReadOperation.LIST_DYNAMIC_PRICING_RULES,
                repository::listDynamicPricingRules
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.dynamic_pricing.manage")
    @Transactional
    public TicketDynamicPricingRuleRecord createDynamicPricingRule(TicketDynamicPricingRuleRequest request) {
        return operations.executeWrite(
                "createTicketDynamicPricingRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                TicketingWriteOperation.CREATE_DYNAMIC_PRICING_RULE,
                () -> pricingService.createDynamicPricingRule(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.dynamic_pricing.publish", admissionAction = "ticketing.dynamic_pricing_publish", objectType = "ticket_dynamic_pricing_rule")
    @Transactional
    public TicketDynamicPricingRuleRecord publishDynamicPricingRule(String ruleId, StatusChangeRequest request) {
        String id = operations.requireText(ruleId, "dynamic_pricing_rule_id");
        return operations.executeWrite(
                "publishTicketDynamicPricingRule:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                TicketingWriteOperation.PUBLISH_DYNAMIC_PRICING_RULE,
                () -> pricingService.publishDynamicPricingRule(id, request)
        );
    }

    @Transactional
    public TicketDynamicPricingSimulationResponse simulateDynamicPricing(TicketDynamicPricingSimulationRequest request) {
        return operations.executeWrite(
                "simulateTicketDynamicPricing",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingSimulationResponse.class,
                TicketingWriteOperation.SIMULATE_DYNAMIC_PRICING,
                () -> pricingService.simulateDynamicPricing(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord createTicketType(TicketTypeRequest request) {
        return operations.executeWrite(
                "createTicketType",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.CREATE_TICKET_TYPE,
                () -> catalogService.createTicketType(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketPriceRuleRecord createTicketPriceRule(TicketPriceRuleRequest request) {
        return operations.executeWrite(
                "createTicketPriceRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                TicketingWriteOperation.CREATE_TICKET_PRICE_RULE,
                () -> catalogService.createPriceRule(request)
        );
    }

    /** 当前操作者 id（用于存档/删除申请留痕与自审校验）；测试无鉴权上下文时回退 null。 */
    private String actorId() {
        if (authorizationService == null) {
            return null;
        }
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            // 无鉴权上下文(测试/内部调用)时静默回退 null;留痕字段与自审校验按缺席处理。
            return null;
        }
    }

    public PageResponse<EntitlementTemplateRecord> listEntitlementTemplates(boolean includeArchived) {
        return operations.readPage(
                TicketingReadOperation.LIST_ENTITLEMENT_TEMPLATES,
                () -> repository.listEntitlementTemplates(includeArchived)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord archiveEntitlementTemplate(String id, EntitlementTemplateArchiveRequest request) {
        String templateId = operations.requireText(id, "id");
        return operations.executeWrite(
                "archiveEntitlementTemplate:" + templateId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.ARCHIVE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.archiveEntitlementTemplate(templateId, true, request == null ? null : request.reason(), actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord unarchiveEntitlementTemplate(String id, EntitlementTemplateArchiveRequest request) {
        String templateId = operations.requireText(id, "id");
        return operations.executeWrite(
                "unarchiveEntitlementTemplate:" + templateId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.UNARCHIVE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.archiveEntitlementTemplate(templateId, false, null, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord requestDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteRequest request) {
        String templateId = operations.requireText(id, "id");
        return operations.executeWrite(
                "requestDeleteEntitlementTemplate:" + templateId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.REQUEST_DELETE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.requestDeleteEntitlementTemplate(templateId, request, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.approve")
    @Transactional
    public EntitlementTemplateDeleteDecision approveDeleteEntitlementTemplate(String id, EntitlementTemplateDeleteApprovalRequest request) {
        String templateId = operations.requireText(id, "id");
        return operations.executeWrite(
                "approveDeleteEntitlementTemplate:" + templateId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateDeleteDecision.class,
                TicketingWriteOperation.APPROVE_DELETE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.approveDeleteEntitlementTemplate(templateId, request, actorId())
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord createEntitlementTemplate(EntitlementTemplateRequest request) {
        return operations.executeWrite(
                "createEntitlementTemplate",
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.CREATE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.createEntitlementTemplate(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "bindTicketTypeEntitlement:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeEntitlementRecord.class,
                TicketingWriteOperation.BIND_TICKET_TYPE_ENTITLEMENT,
                () -> catalogService.bindTicketTypeEntitlement(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public IssuedTicketResponse issueTicketTypeDirectly(String ticketTypeId, TicketIssueRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "issueTicketTypeDirectly:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                TicketingWriteOperation.ISSUE_TICKET_TYPE_DIRECTLY,
                () -> orderService.issueEntitlements(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public TicketOrderRecord createTicketOrder(TicketOrderRequest request) {
        return operations.executeWrite(
                "createTicketOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                TicketingWriteOperation.CREATE_TICKET_ORDER,
                () -> orderService.createOrder(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public TicketPaymentIntentRecord createTicketOrderPayIntent(String orderId, PaymentIntentRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "createTicketOrderPayIntent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPaymentIntentRecord.class,
                TicketingWriteOperation.CREATE_TICKET_ORDER_PAY_INTENT,
                () -> orderService.createPaymentIntent(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public TicketOrderRecord confirmTicketOrderPayment(String orderId, ConfirmTicketOrderPaymentRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "confirmTicketOrderPayment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                TicketingWriteOperation.CONFIRM_TICKET_ORDER_PAYMENT,
                () -> orderService.confirmPayment(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public TicketOrderRecord cancelTicketOrder(String orderId, StatusChangeRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "cancelTicketOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                TicketingWriteOperation.CANCEL_TICKET_ORDER,
                () -> orderService.cancelOrder(id)
        );
    }

    // 系统定时任务入口（2a-5）：取消超时未支付订单并回补库存。无认证上下文/无审计包装，由 TicketOrderExpiryJob 触发。
    @Transactional
    public int cancelExpiredUnpaidTicketOrders(java.time.Instant cutoff) {
        return orderService.cancelExpiredUnpaidOrders(cutoff);
    }

    /**
     * 系统定时任务入口（F-TXN-03 门票生命周期）：处理已过期、全未核销的权益——
     * <ul>
     *   <li>到期**未核销票逐张**原渠道自动退款（系统直批，{@code autoRefundExpiredUnusedTickets}），**不因同单有核销而连坐作废**；
     *       0 元 / 非 paid / 已有退款在途 → 作废或交由退款链；</li>
     *   <li>该单**已有核销** → 其余到期未用权益**作废**（forfeited、不退款，与"核销后不可退"一致）。</li>
     * </ul>
     * 无认证上下文，由 TicketOrderExpiryJob 触发；逐订单独立原子（autoRefund {@code @Transactional} / forfeit 单 UPDATE）。
     * 返回处理的权益数。
     */
    public int processExpiredPaidEntitlements(java.time.Instant now) {
        List<IssuedEntitlementRecord> expired = repository.listExpiredUnusedActiveEntitlements(now);
        java.util.Map<String, java.util.List<IssuedEntitlementRecord>> byOrder = new java.util.LinkedHashMap<>();
        int processed = 0;
        for (IssuedEntitlementRecord e : expired) {
            if (e.orderId() == null || e.orderId().isBlank()) {
                processed += repository.forfeitEntitlement(e.id(), now); // 无订单（直接发放）到期未用 → 作废。
                continue;
            }
            byOrder.computeIfAbsent(e.orderId(), k -> new java.util.ArrayList<>()).add(e);
        }
        for (var entry : byOrder.entrySet()) {
            String orderId = entry.getKey();
            // F-TXN-03 按票实例（2026-06-29 裁决）：到期未核销票逐张原渠道自动退款，**不再因同单有核销而连坐作废**；
            // 已有退款在途/完成则交由退款链、跳过。
            TicketOrderRecord order = repository.findTicketOrderById(orderId).orElse(null);
            if (order != null && (repository.sumApprovedRefundAmountByOrderId(orderId) > 0
                    || repository.sumPendingRefundAmountByOrderId(orderId) > 0)) {
                continue;
            }
            TicketRefundRecord refund = refundService.autoRefundExpiredUnusedTickets(order, entry.getValue(), now);
            if (refund != null) {
                processed += entry.getValue().size();
            } else {
                // 0 元 / 非 paid 无可退 → 作废到期未用权益，避免反复扫描。
                for (IssuedEntitlementRecord e : entry.getValue()) {
                    processed += repository.forfeitEntitlement(e.id(), now);
                }
            }
        }
        return processed;
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.ticket.issue")
    @Transactional
    public IssuedTicketResponse issueTicketOrder(String orderId, StatusChangeRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "issueTicketOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                TicketingWriteOperation.ISSUE_TICKET_ORDER,
                () -> orderService.issueOrder(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "publishTicketType:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.PUBLISH_TICKET_TYPE,
                () -> catalogService.publishTicketType(id, request)
        );
    }

    public PageResponse<IssuedEntitlementRecord> listIssuedEntitlements() {
        return operations.readPage(
                TicketingReadOperation.LIST_ISSUED_ENTITLEMENTS,
                () -> filterByMerchantScope(repository.listIssuedEntitlements(), IssuedEntitlementRecord::merchantId)
        );
    }

    public PageResponse<TicketOrderRecord> listTicketOrders() {
        // 行级隔离（V135）：按调用方 scope 过滤——平台看全部、商户仅见自身受理订单（堵 ticket.order.read 下放后的跨租户泄露）。
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_ORDERS,
                orderService::listOrdersInScope
        );
    }

    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return repository.listTicketOrdersByCustomerId(operations.requireText(customerId, "customer_id"));
    }

    /** 1.8 客人端我的票：校验订单归属后列该订单全部票实例（安全投影，含票种名）。 */
    public List<CustomerTicketResponse> listCustomerOrderTickets(String orderId, String customerId) {
        String resolvedOrderId = operations.requireText(orderId, "order_id");
        String resolvedCustomerId = operations.requireText(customerId, "customer_id");
        TicketOrderRecord order = repository.findTicketOrderById(resolvedOrderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
        if (!resolvedCustomerId.equals(order.customerId())) {
            throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "order does not belong to customer");
        }
        String ticketName = repository.findTicketTypeById(order.ticketTypeId())
                .map(TicketTypeRecord::ticketName).orElse(null);
        return repository.listIssuedEntitlementsByOrderId(resolvedOrderId).stream()
                .map(entitlement -> CustomerTicketResponse.of(entitlement, ticketName))
                .toList();
    }

    @Transactional
    public TicketCredentialResponse getTicketCredential(String ticketId) {
        return credentialService.getTicketCredential(ticketId);
    }

    @Transactional
    public TicketCredentialResponse getCustomerTicketCredential(String ticketId, String customerId) {
        return credentialService.getCustomerTicketCredential(ticketId, customerId);
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.request")
    @Transactional
    public TicketRefundRecord requestTicketRefund(TicketRefundRequest request) {
        return operations.executeWrite(
                "requestTicketRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.REQUEST_TICKET_REFUND,
                () -> refundService.requestRefund(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.approve", admissionAction = "ticketing.refund_approve", objectType = "ticket_refund")
    @Transactional
    public TicketRefundRecord approveTicketRefund(String refundId, StatusChangeRequest request) {
        String id = operations.requireText(refundId, "refund_id");
        return operations.executeWrite(
                "approveTicketRefund:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.APPROVE_TICKET_REFUND,
                () -> refundService.approveRefund(id, request)
        );
    }

    public PageResponse<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
        String id = operations.requireText(refundId, "refund_id");
        // B1d.3 跨租户读隔离：退款行无 merchant_id，按其 order 的受理商户过滤；
        // 商户 scope 仅见自身订单的退款行，平台 scope（empty）见全部。
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_REFUND_LINES,
                () -> filterByMerchantScope(repository.listTicketRefundLines(id),
                        line -> repository.findTicketOrderById(line.orderId())
                                .map(TicketOrderRecord::merchantId).orElse(null))
        );
    }

    public OfflinePackageResponse getOfflineEntitlements(String merchantId, String venueId, int limit) {
        return offlinePackageService.getOfflineEntitlements(merchantId, venueId, limit);
    }

    @Transactional
    public VerificationPreviewResponse previewVerification(VerificationRequest request) {
        return operations.executeWrite(
                "previewTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationPreviewResponse.class,
                TicketingWriteOperation.PREVIEW_VERIFICATION,
                () -> verificationService.previewVerification(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "verification.execute")
    @Transactional
    public VerificationResponse createVerification(VerificationRequest request) {
        return operations.executeWrite(
                "createTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                TicketingWriteOperation.CREATE_VERIFICATION,
                () -> verificationService.createVerification(request)
        );
    }

    /** C1/REQ-05：人工放行（主管），留痕 status=manual_overridden。权限在端点层按主管权限码控制。 */
    @com.complex.platform.gate.GuardedAction(admissionAction = "ticketing.special_release", objectType = "verification")
    public VerificationResponse manualOverrideVerification(VerificationRequest request, String reason) {
        return verificationService.manualOverride(request, reason);
    }

    /** R5 T5：核销冲正。幂等键必填、reason 必填、权限 ticket.verify.reverse；成功事件 ticket.verification_reversed。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.verify.reverse", admissionAction = "ticketing.verification_reverse", objectType = "verification")
    @Transactional
    public VerificationResponse reverseVerification(String verificationId, VerificationReverseRequest request, String reversedBy) {
        return operations.executeWrite(
                "reverseTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                TicketingWriteOperation.REVERSE_VERIFICATION,
                () -> verificationService.reverseVerification(
                        verificationId,
                        request == null ? null : request.reason(),
                        reversedBy
                )
        );
    }

    public PageResponse<VerificationRecord> listVerifications() {
        return operations.readPage(
                TicketingReadOperation.LIST_VERIFICATIONS,
                () -> filterByMerchantScope(repository.listVerifications(), VerificationRecord::merchantId)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "terminal.device.bind")
    @Transactional
    public SyncTasksResponse syncTerminalTasks(TerminalSyncTasksRequest request) {
        return operations.executeWrite(
                "syncTerminalTasks",
                request == null ? null : request.idempotencyKey(),
                request,
                SyncTasksResponse.class,
                TicketingWriteOperation.SYNC_TERMINAL_TASKS,
                () -> verificationService.syncTerminalTasks(request)
        );
    }
}
