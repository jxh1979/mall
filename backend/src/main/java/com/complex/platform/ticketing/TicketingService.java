package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.EntitlementAgreementPriceRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateArchiveRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.QrRefreshConfigRequest;
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
    // AUD-077 转赠链(可空=既有单元测试双子用 InMemory repo,不走转赠;生产由 @Autowired 主构造注入)。
    private final TicketTransferService transferService;

    @org.springframework.beans.factory.annotation.Autowired
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
            AuthorizationService authorizationService,
            TicketTransferService transferService
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
        this.transferService = transferService;
    }

    // 兼容既有单元测试(InMemory repo,不需要转赠链)。
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
        this(repository, catalogService, pricingService, orderService, refundService, verificationService,
                credentialService, offlinePackageService, operations, authorizationService, null);
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

    /** S5 票面赠酒清单：该票种各酒吧分别赠几杯（售票时写明票面）。与 K17 发布校验同一份数据。 */
    public PageResponse<TicketTypeGiftCoverageRow> listGiftCoverage(String ticketTypeId) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_TYPES,
                () -> catalogService.listGiftCoverage(id)
        );
    }

    /** V359：要求购票人拍入场人脸照的票种 id 集合（供客人端购票页判定是否弹拍照）。 */
    public java.util.Set<String> ticketTypeIdsRequiringFacePhoto() {
        return repository.listTicketTypeIdsRequiringFacePhoto();
    }

    /**
     * V359：当前生效的人脸照留存天数（后台 ADM-ANA-011 可改，缺配置 30）。
     * 客人端拍照前要把「照片留多久」如实告诉客人，写死数字会在运营改配置后变成假告知。
     */
    public int facePhotoRetentionDays() {
        return orderService.facePhotoRetentionDays();
    }

    /**
     * V368：当前这一版拍照告知的版本号（模板版本 + 当时告知的留存天数）。
     * 客人勾同意时把它写进同意台账，将来才答得出「这位客人同意的是哪一版告知」。
     */
    public String facePhotoNoticeVersion() {
        return orderService.facePhotoNoticeVersion();
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

    /** #172(2026-07-19 拍板)：读全场统一动态码刷新间隔（秒）。 */
    public QrRefreshConfigResponse getQrRefreshConfig() {
        return new QrRefreshConfigResponse(repository.findQrRefreshIntervalSeconds());
    }

    /** #172：改全场统一动态码刷新间隔（秒），须能被 5 整除且 5≤x≤60；走 executeWrite 幂等+审计。 */
    public QrRefreshConfigResponse updateQrRefreshConfig(QrRefreshConfigRequest request) {
        return operations.executeWrite(
                "updateQrRefreshConfig",
                request == null ? null : request.idempotencyKey(),
                request,
                QrRefreshConfigResponse.class,
                TicketingWriteOperation.UPDATE_QR_REFRESH_CONFIG,
                () -> {
                    Integer value = request == null ? null : request.refreshIntervalSeconds();
                    if (value == null || value % 5 != 0 || value < 5 || value > 60) {
                        throw com.complex.platform.common.api.ApiException.badRequest(
                                "VALIDATION_FAILED",
                                "refresh_interval_seconds must be a multiple of 5 between 5 and 60");
                    }
                    repository.upsertQrRefreshIntervalSeconds(value, actorId(), java.time.Instant.now());
                    return new QrRefreshConfigResponse(value);
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketDistributionMerchantRateRecord createDistributionMerchantRate(
            com.complex.platform.ticketing.dto.TicketDistributionMerchantRateRequest request) {
        if (request == null || request.merchantId() == null || request.merchantId().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id is required");
        }
        if (request.perGuestCent() == null && request.fixedAmountCent() == null) {
            throw ApiException.badRequest(
                    "VALIDATION_FAILED", "per_guest_cent or fixed_amount_cent is required");
        }
        java.time.Instant now = java.time.Instant.now();
        String id = "dmr_" + distributionRateHash(request.projectId(), request.merchantId());
        return repository.saveDistributionMerchantRate(new TicketDistributionMerchantRateRecord(
                id, request.projectId(), request.merchantId(), request.perGuestCent(), request.fixedAmountCent(),
                request.status() == null || request.status().isBlank() ? "active" : request.status(),
                Boolean.TRUE.equals(request.isDraft()), actorId(), now));
    }

    private static String distributionRateHash(String projectId, String merchantId) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((String.valueOf(projectId) + ":" + merchantId).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .substring(0, 32);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("distribution rate id hashing unavailable", e);
        }
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

    /**
     * 配置中心批5：改虚拟权益签约价（= 内部结算价）。走 executeWrite 拿幂等 + 审计留痕。
     * 只写模板一行——已发放/已核销的按各自快照，改价碰不到（见 TicketCatalogService#updateAgreementPrice）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord updateEntitlementTemplateAgreementPrice(
            String id, EntitlementAgreementPriceRequest request) {
        String templateId = operations.requireText(id, "id");
        return operations.executeWrite(
                "updateEntitlementTemplateAgreementPrice:" + templateId,
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.UPDATE_ENTITLEMENT_TEMPLATE_AGREEMENT_PRICE,
                () -> catalogService.updateAgreementPrice(templateId, request, actorId())
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
        // AUD-077 票到期先终止在途转赠(pending→voided,解冻退回原持票人),再走既有到期退款/作废规则。
        if (transferService != null) {
            java.util.Set<String> expiringParents = new java.util.LinkedHashSet<>();
            for (IssuedEntitlementRecord e : expired) {
                if (e.parentTicketId() != null) {
                    expiringParents.add(e.parentTicketId());
                }
            }
            for (String parentId : expiringParents) {
                transferService.voidPendingTransfersForParent(parentId, "ticket_expired", now);
            }
        }
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
            java.util.List<IssuedEntitlementRecord> expiredUnused = entry.getValue();
            // AUD-011：不再因「订单已有退款」整单跳过——同单人工退过 1 张后，其余到期票此前永远既不退也不作废。
            // 幂等粒度=父票实例（refunded 实例的权益已冻结不在待处理集合，实例级 CAS 防并发双退）。
            // 复核修正（P1-B）：人工退款「申请即冻结」只冻父实例（refund_pending），子权益要到审批才冻结——
            // 这里作废它会让审批冻结命中 0 行、退款单永远批不了（驳回后实例回 active 但权益已作废=死票）。
            // 该类权益留给退款链处置（批准→随整票冻结；驳回→回 active 后由下轮到期作业正常处置）。
            TicketOrderRecord order = repository.findTicketOrderById(orderId).orElse(null);
            java.util.Set<String> refundPendingParents = repository.listTicketInstancesByOrderId(orderId).stream()
                    .filter(ti -> "refund_pending".equals(ti.status()))
                    .map(TicketInstanceRecord::id)
                    .collect(java.util.stream.Collectors.toSet());
            if (!refundPendingParents.isEmpty()) {
                expiredUnused = expiredUnused.stream()
                        .filter(e -> e.parentTicketId() == null || !refundPendingParents.contains(e.parentTicketId()))
                        .toList();
                if (expiredUnused.isEmpty()) {
                    continue;
                }
            }
            // 规则2 可退配置：票种默认不可退 → 到期直接作废不退（客人到期无退款）。
            TicketTypeRecord ticketType = order == null
                    ? null : repository.findTicketTypeById(order.ticketTypeId()).orElse(null);
            boolean refundable = ticketType != null && ticketType.refundable();
            if (!refundable) {
                for (IssuedEntitlementRecord e : expiredUnused) {
                    processed += repository.forfeitEntitlement(e.id(), now);
                }
                continue;
            }
            // 可退票整票口径（方案2）：完全未核销的父票实例整张自动退；部分使用实例的到期未核销权益 → 作废不退。
            TicketRefundRecord refund = refundService.autoRefundExpiredWholeTickets(order, now);
            java.util.Set<String> refundedEntIds = refund == null ? java.util.Set.of()
                    : repository.listTicketRefundLines(refund.id()).stream()
                            .map(TicketRefundLineRecord::entitlementId)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.toSet());
            for (IssuedEntitlementRecord e : expiredUnused) {
                if (refundedEntIds.contains(e.id())) {
                    processed += 1; // 已随整票退款冻结，计入处理数。
                    continue;
                }
                processed += repository.forfeitEntitlement(e.id(), now); // 部分使用实例的剩余到期票 → 作废不退。
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

    /** AUD-076 展示层实时解析酒吧名（供客人端商品/订单/我的票展示；ID 快照、名字实时 join）。 */
    public String resolveBarName(String barId) {
        return catalogService.resolveBarName(barId);
    }

    /**
     * AUD-076 酒吧撤场提前到期：把该酒吧单店票已售权益 valid_to 提前到截止营业时刻（effectiveAt），
     * 由 processExpiredPaidEntitlements 走方案2到期规则处置（可退未用自动退/不可退作废）。返回受影响权益数。
     * 由 PlatformService.archiveMerchant（撤场=商户存档）在被存档商户为酒吧时调用。
     */
    @Transactional
    public int expireBarSingleStoreTickets(String barId, java.time.Instant effectiveAt) {
        if (barId == null || barId.isBlank()) {
            return 0;
        }
        return repository.expireBarEntitlements(barId, effectiveAt, java.time.Instant.now());
    }

    public PageResponse<IssuedEntitlementRecord> listIssuedEntitlements() {
        return operations.readPage(
                TicketingReadOperation.LIST_ISSUED_ENTITLEMENTS,
                () -> filterByMerchantScope(repository.listIssuedEntitlements(), IssuedEntitlementRecord::merchantId)
        );
    }

    // AUD-066 父票实例查询（后台核销台账/退款审阅锚点）：沿 ticket.verify.view 权限、project scope。
    public PageResponse<TicketInstanceRecord> listTicketInstances() {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_INSTANCES,
                repository::listTicketInstances
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
        // AUD-066：父票号供"我的票"按父票聚合展示（parent_ticket_id -> ticket_no）。
        java.util.Map<String, String> ticketNoByParent = repository.listTicketInstancesByOrderId(resolvedOrderId).stream()
                .collect(java.util.stream.Collectors.toMap(TicketInstanceRecord::id, TicketInstanceRecord::ticketNo, (a, b) -> a));
        return repository.listIssuedEntitlementsByOrderId(resolvedOrderId).stream()
                .map(entitlement -> CustomerTicketResponse.of(entitlement, ticketName,
                        entitlement.parentTicketId() == null ? null : ticketNoByParent.get(entitlement.parentTicketId())))
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

    // ── AUD-077 门票转赠(客人端经 CustomerService 委派;鉴权=客人 token,已在 CustomerService 过账号闸) ──
    /** AUD-006 幂等:必填键+请求指纹+响应精确重放(丢响应重试可拿回同一 claim_code);operationId 绑定客人,键不跨客人复用。 */
    public TicketTransferResponse initiateTicketTransfer(String customerId, String customerPhone,
            com.complex.platform.customer.dto.CustomerTicketTransferRequest request) {
        return operations.executeWrite(
                "initiateTicketTransfer:" + customerId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTransferResponse.class,
                TicketingWriteOperation.INITIATE_TICKET_TRANSFER,
                () -> transferService.initiate(customerId, customerPhone, request));
    }

    /** AUD-006 幂等:operationId 绑定客人+转赠单;请求指纹=领取请求体。 */
    public TicketTransferResponse claimTicketTransfer(String customerId, String customerPhone, String transferId,
            com.complex.platform.customer.dto.CustomerTicketTransferClaimRequest request) {
        return operations.executeWrite(
                "claimTicketTransfer:" + customerId + ":" + transferId,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTransferResponse.class,
                TicketingWriteOperation.CLAIM_TICKET_TRANSFER,
                () -> transferService.claim(customerId, customerPhone, transferId,
                        request == null ? null : request.claimCode()));
    }

    public TicketTransferResponse revokeTicketTransfer(String customerId, String transferId) {
        return transferService.revoke(customerId, transferId);
    }

    public TicketTransferPreviewResponse previewTicketTransfer(String claimCode) {
        return transferService.preview(claimCode);
    }

    public List<TicketTransferResponse> listIncomingTicketTransfers(String customerPhone) {
        return transferService.listIncoming(customerPhone);
    }

    public List<TicketTransferResponse> listOutgoingTicketTransfers(String customerId) {
        return transferService.listOutgoing(customerId);
    }

    /** AUD-077 我的票(持有人视角):holder=me 的全部 slot(自购未转+受赠),含转赠中("transferring")标记。 */
    public List<CustomerTicketResponse> listCustomerHeldTickets(String customerId) {
        String me = operations.requireText(customerId, "customer_id");
        List<CustomerTicketResponse> out = new java.util.ArrayList<>();
        for (TicketSlotHolderRecord holder : repository.listSlotHoldersByHolder(me)) {
            TicketInstanceRecord parent = repository.findTicketInstanceById(holder.parentTicketId()).orElse(null);
            String ticketNo = parent == null ? null : parent.ticketNo();
            String ticketName = parent == null ? null : repository.findTicketTypeById(parent.ticketTypeId())
                    .map(TicketTypeRecord::ticketName).orElse(null);
            String transferState = repository.existsPendingTransferForSlot(holder.parentTicketId(), holder.slotIndex())
                    ? "transferring" : null;
            for (IssuedEntitlementRecord e : repository.listIssuedEntitlementsByParentSlot(holder.parentTicketId(), holder.slotIndex())) {
                out.add(CustomerTicketResponse.ofWithTransfer(e, ticketName, ticketNo, transferState));
            }
        }
        return out;
    }

    /** AUD-077 后台客服只读转赠记录(纠纷排查,首期只读不撤回)。权限 ticket.transfer.read。 */
    public PageResponse<TicketTransferResponse> listTicketTransfers() {
        return operations.readPage(TicketingReadOperation.LIST_TICKET_TRANSFERS, transferService::listAllForAdmin);
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

    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.approve", admissionAction = "ticketing.refund_reject", objectType = "ticket_refund")
    @Transactional
    public TicketRefundRecord rejectTicketRefund(String refundId, StatusChangeRequest request) {
        String id = operations.requireText(refundId, "refund_id");
        return operations.executeWrite(
                "rejectTicketRefund:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.REJECT_TICKET_REFUND,
                () -> refundService.rejectRefund(id, request)
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
