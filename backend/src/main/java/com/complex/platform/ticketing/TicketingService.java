package com.complex.platform.ticketing;

// 文件版本：V0.09

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
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.QrRefreshConfigRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.dto.VerificationAnomalyResolveRequest;
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
    private final TicketingOperationExecutor operations;
    // P1 越权修复：核销/权益列表按调用者商户范围过滤（记录含 merchant_id）。可为 null=兼容既有测试构造（不过滤）。
    private final AuthorizationService authorizationService;
    // AUD-077 转赠链(可空=既有单元测试双子用 InMemory repo,不走转赠;生产由 @Autowired 主构造注入)。
    private final TicketTransferService transferService;
    // TK-05-R2/R3 通票安全例外（字段注入免改既有构造点；生产恒注入）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    TicketSafetyExceptionService safetyExceptionService;
    // 第十八批 N 段第 123 条 123-R2：敏感操作定位闸（字段注入免改构造点；单测未注入即不查）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.gate.SensitiveLocationGuard locationGuard;
    // 第十八批 N 段第 120 条 120-R2：一体机售票下单留痕记终端、设备与点位（字段注入免改构造点；单测未注入即不记）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.terminal.TerminalDeviceService terminalDeviceService;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketingService(
            TicketingRepository repository,
            TicketCatalogService catalogService,
            TicketPricingService pricingService,
            TicketOrderService orderService,
            TicketRefundService refundService,
            TicketVerificationService verificationService,
            TicketCredentialService credentialService,
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
        this.operations = operations;
        this.authorizationService = authorizationService;
        this.transferService = transferService;
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

    /**
     * V369 票种定价核算：各业态票面原价合计 × 票种折扣 = 系统算出售价，与运营填的售价差多少、超没超允许误差。
     * 建票/改票页拿它做实时提示，发布闸拒绝时报的是同一份计算。
     */
    public TicketTypePriceCheck ticketTypePriceCheck(String ticketTypeId) {
        return catalogService.priceCheck(operations.requireText(ticketTypeId, "ticket_type_id"));
    }

    /** V369 改票种售价／折扣：保存当场比对允许误差，超出即拒（不等到发布才报错）。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord updateTicketTypePricing(String ticketTypeId,
            com.complex.platform.ticketing.dto.TicketTypePricingRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "updateTicketTypePricing:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.UPDATE_TICKET_TYPE_PRICING,
                () -> catalogService.updatePricing(id, request)
        );
    }

    /** 第十八批 AH 段 215-R2 票种最低赠酒杯数（没配过的按出厂 1 杯）。 */
    public TicketTypeGiftFloor getGiftFloor(String ticketTypeId) {
        return catalogService.getGiftFloor(operations.requireText(ticketTypeId, "ticket_type_id"));
    }

    /** 215-R2 改票种最低赠酒杯数（杯数与上下限同配）：成功留痕记改前改后；已上架通票跌破即退回草稿。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeGiftFloor configureGiftFloor(String ticketTypeId,
            com.complex.platform.ticketing.dto.TicketTypeGiftFloorRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        java.util.Map<String, Object> trail = new java.util.LinkedHashMap<>();
        return operations.executeWrite(
                "configureTicketTypeMinGiftCups:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeGiftFloor.class,
                TicketingWriteOperation.CONFIGURE_TICKET_TYPE_MIN_GIFT_CUPS,
                () -> {
                    trail.put("min_gift_cups_before", catalogService.getGiftFloor(id).minGiftCups());
                    TicketTypeGiftFloor saved = catalogService.configureGiftFloor(id, request, actorId());
                    trail.put("min_gift_cups", saved.minGiftCups());
                    trail.put("min_gift_cups_lower", saved.minGiftCupsLower());
                    trail.put("min_gift_cups_upper", saved.minGiftCupsUpper());
                    return saved;
                },
                trail);
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
                () -> pricingService.simulateDynamicPricing(request, orderService::saleBusinessDate)
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
                                "刷新间隔秒数必须为 5 到 60 之间且为 5 的倍数");
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
        // S8/V374 二选一:同行双值无从消歧(两段化后没有项目级 method 兜底),落库前拦住并直说该怎么填。
        if ((request.perGuestCent() == null) == (request.fixedAmountCent() == null)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "每客单价(per_guest_cent)与固定额(fixed_amount_cent)必须二选一:该商户按哪种固定规则分账,只能配一种");
        }
        String ruleType = request.perGuestCent() != null ? "per_guest" : "fixed";
        java.time.Instant now = java.time.Instant.now();
        String id = "dmr_" + distributionRateHash(request.projectId(), request.merchantId());
        return repository.saveDistributionMerchantRate(new TicketDistributionMerchantRateRecord(
                id, request.projectId(), request.merchantId(), ruleType, request.perGuestCent(), request.fixedAmountCent(),
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

    // 批10/E：updateEntitlementTemplateAgreementPrice(配置中心批5/PR#274)已下线——改价收编进内部结算 SKU 目录。

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
                () -> orderService.createOrder(request),
                kioskSaleMetadata(request)
        );
    }

    /**
     * 第十八批 N 段第 120 条 120-R2（BSM:999「……并把购票、支付、出票和售票原因写入留痕。」；BSM:1947「……记录服务人员、设备、点位、购票原因、支付和出票。」）：
     * 一体机售票的下单留痕记售票原因与可信终端、设备号、绑定点位（服务人员＝留痕操作人；付款、出票留痕沿用各自事件）；其他渠道不记。执行前组好。
     */
    private java.util.Map<String, Object> kioskSaleMetadata(TicketOrderRequest request) {
        if (request == null || !"kiosk".equals(com.complex.platform.common.Strings.trimToNull(request.salesChannel()))
                || terminalDeviceService == null) {
            return java.util.Map.of();
        }
        java.util.Map<String, Object> metadata = terminalDeviceService.terminalTraceMetadata();
        String reason = com.complex.platform.common.Strings.trimToNull(request.operatorNote());
        if (reason != null) {
            metadata.put("sale_reason", reason);
        }
        return metadata;
    }

    /**
     * ROOT-03-R27 客人自助下单：不收价格规则，由系统在客人符合的资格价（eligibleVariants，客人域判定）与动态公开价中取最低，订单另存全部命中资格价。
     */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public TicketOrderRecord createCustomerTicketOrder(TicketOrderRequest request, java.util.Set<String> eligibleVariants) {
        return operations.executeWrite(
                "createTicketOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                TicketingWriteOperation.CREATE_TICKET_ORDER,
                () -> orderService.createOrder(request, java.util.Objects.requireNonNull(eligibleVariants))
        );
    }

    /** ROOT-03-R27：客人此刻买某票种的价格（基础价、命中资格价、最终价），供票种列表展示「我的价格」。visitDate 为空＝票种当前营业日。 */
    public TicketPricingService.CustomerPrice quoteCustomerPrice(String ticketTypeId, java.time.LocalDate visitDate,
            String salesChannel, java.util.Set<String> eligibleVariants) {
        TicketTypeRecord ticketType = repository.findTicketTypeById(operations.requireText(ticketTypeId, "ticket_type_id"))
                .orElseThrow(() -> ApiException.notFound("TICKET_TYPE_NOT_FOUND", "票种不存在或已被删除"));
        return pricingService.resolveCustomerPrice(ticketType,
                visitDate == null ? orderService.saleBusinessDate(ticketType) : visitDate, salesChannel, eligibleVariants);
    }

    /** ROOT-03-R4：客人本人订单发起在线付款（金额＝待付金额，服务端定）。 */
    @Transactional
    public com.complex.platform.finance.PaymentIntentRecord createCustomerOnlinePayment(String orderId, String customerId,
            String channel, String idempotencyKey) {
        return orderService.createCustomerOnlinePayment(orderId, customerId, channel, idempotencyKey);
    }

    /** ROOT-03-R4/R17/R18：在线到账确认（客人回前台确认与服务端结算作业共用；customerId 为空＝作业）。 */
    @Transactional
    public TicketOrderService.OnlineSettlement settleCustomerOnlinePayment(String orderId, String paymentIntentId, String customerId) {
        return orderService.settleOnlinePayment(orderId, paymentIntentId, customerId);
    }

    /** ROOT-03-R20：订单已收各笔（只列成功的，按付款先后）。 */
    public List<TicketPaymentIntentRecord> listOrderPayments(String orderId) {
        return repository.listTicketPaymentIntentsByOrder(operations.requireText(orderId, "order_id")).stream()
                .filter(intent -> "succeeded".equals(intent.status())).toList();
    }

    /** ROOT-03-R27：订单命中的全部资格价。 */
    public List<java.util.Map<String, Object>> listOrderPriceHits(String orderId) {
        return repository.findTicketOrderPriceHits(operations.requireText(orderId, "order_id"));
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

    /**
     * 第十八批 N 段第 123 条 123-R2（BSM:1630）：现场确认票款（控制器入口）先过敏感操作定位闸再走原方法。第三方通票回流
     * （TicketingChannelTicketIssueAdapter）直接调原方法登记渠道收款：客人已在渠道付讫、没有本系统的操作设备，不查位置。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public TicketOrderRecord confirmTicketOrderPaymentOnSite(String orderId, ConfirmTicketOrderPaymentRequest request) {
        if (locationGuard != null) {
            locationGuard.check("confirmTicketOrderPayment",
                    () -> repository.findTicketOrderById(orderId).map(TicketOrderRecord::projectId).orElse(null));
        }
        return confirmTicketOrderPayment(orderId, request);
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
     * 系统定时任务入口（F-TXN-03 门票生命周期）：已过期、全未核销的权益**一律作废**（forfeited），**不产生任何退款**。
     *
     * <p>业务权威口径：{@code business-enum-quick-reference-v0.1.html}「售票与退款原则」行——门票售出后原则上不退款，
     * <b>未到场、忘记使用或主动放弃都不是退款理由</b>；同文件「到期未核销款」行——到期未核销的票先且只扣一次第三方渠道费用，
     * 余额按票种经营域归属。{@code business-system-map-for-operations-v0.1.html}「到期处理」行同义。
     * 业务口径从未授权「到期自动退」，故此处只作废、不退款（旧实现对 refundable 票种调 autoRefundExpiredWholeTickets，已删）。</p>
     *
     * <p>票种 {@code refundable} 只表示「客人可申请整票退款」，与本作业无关：客人仍走人工申请 → 审批 → 确认拨付的退款链。
     * 人工退款审批中（父实例 refund_pending、子权益仍 active）的权益留给退款链处置，本作业跳过。</p>
     *
     * <p>无认证上下文，由 TicketOrderExpiryJob 触发；forfeit 为单 UPDATE，逐条独立原子。返回处理的权益数。</p>
     */
    public int processExpiredPaidEntitlements(java.time.Instant now) {
        // 第十八批 AH 段 239-R2（ENUM:46「一经完成检票环节……其全部权益的可用窗口收窄为该场次」；ENUM:48「同票已开启的那个场次一经结束即永无法入场，
        // 于该场次结束时按本条处理」；BSM:800「场次结束时仍未核销的权益即行终结」）：父票本场已检票的权益本场内不受票面有效期截断——先剔除，
        // 不作废也不动在途转赠；场次结束（进入下一营业日）后下轮照常处理。
        List<IssuedEntitlementRecord> expired = repository.listExpiredUnusedActiveEntitlements(now).stream()
                .filter(e -> !verificationService.ticketCheckedInThisSession(e, now))
                .toList();
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
            // 到期一律作废、不退款（business-enum「售票与退款原则」：未到场/忘记使用/主动放弃都不是退款理由；
            // 「到期未核销款」：到期票只扣一次渠道费、余额按经营域归属——业务口径从未授权「到期自动退」）。
            // 票种 refundable 只管「客人可申请整票退款」，客人照旧走人工申请退款链，不受本作业影响。
            for (IssuedEntitlementRecord e : expiredUnused) {
                processed += repository.forfeitEntitlement(e.id(), now);
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
    /** ROOT-03 clients-R9（BSM 第 858、879 行）：票种所含权益名称（含赠饮），供客人端门票守则展示。 */
    public List<String> ticketTypeEntitlementNames(String ticketTypeId) {
        return repository.listTicketTypeEntitlements(ticketTypeId).stream()
                .map(binding -> repository.findEntitlementTemplateById(binding.entitlementTemplateId())
                        .map(EntitlementTemplateRecord::name).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

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

    public PageResponse<TicketOrderRecord> listTicketOrders(String buyerPhone) {
        // 行级隔离（V135）：按调用方 scope 过滤——平台看全部、商户仅见自身受理订单（堵 ticket.order.read 下放后的跨租户泄露）。
        // 第十八批 N 段 120-R2：带购票手机号时只列该号订单（一体机订单查询）。
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_ORDERS,
                () -> orderService.listOrdersInScope(buyerPhone)
        );
    }

    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return repository.listTicketOrdersByCustomerId(operations.requireText(customerId, "customer_id"));
    }

    /** 批6：按 id 取票订单（只读，供第三方通票回流适配层对账系统取数；范式同 resolveBarName 跨域只读口）。 */
    public java.util.Optional<TicketOrderRecord> findTicketOrderById(String orderId) {
        return repository.findTicketOrderById(operations.requireText(orderId, "order_id"));
    }

    /** 1.8 客人端我的票：校验订单归属后列该订单全部票实例（安全投影，含票种名）。 */
    public List<CustomerTicketResponse> listCustomerOrderTickets(String orderId, String customerId) {
        String resolvedOrderId = operations.requireText(orderId, "order_id");
        String resolvedCustomerId = operations.requireText(customerId, "customer_id");
        TicketOrderRecord order = repository.findTicketOrderById(resolvedOrderId)
                .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "票务订单不存在或已被删除"));
        if (!resolvedCustomerId.equals(order.customerId())) {
            throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该订单不属于当前客人");
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

    /**
     * ROOT-03-R29 我的票使用记录（BSM 第 862、880 行）：只许本人当前持有的票（人员权益组持有人；无持有人的按订单归属，与取码同口径），
     * 他人 403 CUSTOMER_ORDER_FORBIDDEN。
     */
    public List<CustomerTicketUsageResponse> listCustomerTicketUsages(String ticketId, String customerId) {
        String me = operations.requireText(customerId, "customer_id");
        IssuedEntitlementRecord entitlement = repository.findIssuedEntitlementById(operations.requireText(ticketId, "ticket_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "权益不存在或已被删除"));
        String holder = entitlement.parentTicketId() == null ? null
                : repository.findSlotHolder(entitlement.parentTicketId(), entitlement.slotIndex())
                        .map(TicketSlotHolderRecord::holderCustomerId).orElse(null);
        String owner = holder != null ? holder
                : repository.findTicketOrderById(entitlement.orderId()).map(TicketOrderRecord::customerId).orElse(null);
        if (!me.equals(owner)) {
            throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "该票不属于当前客人");
        }
        return repository.listTicketUsages(entitlement.id());
    }

    /** ROOT-03-R29 第三方渠道接入状态（供客户端置灰）。 */
    public record TicketChannelStatus(String channel, String name, boolean enabled) {
    }

    /**
     * ROOT-03-R29（BSM 第 892 行「第三方团购（美团/大众点评/抖音）购买的票按渠道来源在“我的订单/我的票”统一展示，渠道未接入前相关入口置灰、
     * 不展示伪造数据」）：三个渠道一律读唯一的渠道售票开关（V071 integration.ticket_channel.sync，出厂关），不返回任何订单样例。
     */
    public List<TicketChannelStatus> listTicketChannels() {
        boolean enabled = repository.ticketChannelSyncEnabled();
        return List.of(new TicketChannelStatus("meituan", "美团", enabled), new TicketChannelStatus("dianping", "大众点评", enabled),
                new TicketChannelStatus("douyin", "抖音", enabled));
    }

    /** ROOT-03-R29 注册成功后（同一事务）：把该手机号此前买的、尚未归属会员的第三方渠道票与一体机票归到新会员。 */
    public int linkUnclaimedOrdersToMember(String phone, String customerId) {
        return repository.linkUnclaimedOrdersToMember(phone, customerId, TicketOrderService.MEMBER_LINKABLE_CHANNELS,
                java.time.Instant.now());
    }

    /** 票种所属综合体（PGM 第 736 行按综合体的终端开关）：与下单定订单综合体同一口径，关联多个或无法确定时为空。 */
    public String saleProjectIdOf(String ticketTypeId) {
        return orderService.resolveSaleProjectId(ticketTypeId, false);
    }

    // ROOT-03-R21 注销删人脸绑定（BSM 第 890 行「该人脸在该票用完或到期后删除」，第 863 行「注销后……人脸……一律删除」）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private TicketFacePhotoRepository facePhotos;

    /**
     * ROOT-03-R21（BSM 第 863 行「未核销的票按本文件「退款」条的退票规则处理」「注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除」；
     * 退款条「未到场、忘记使用或主动放弃均不构成退款理由」；2026-10-25 操作员更正「即使注销，门票是不可以退的，除非碰上特殊安全情况」）：
     * 注销与拉黑共用。本人持有的未核销权益一律作废（不退款；含已开始使用的多次票剩余次数——注销后客人已无法出示动态码；已用完的保持「已使用」、不改作废，ENUM 第 41 行「已使用」「已作废」是两种状态；含只持有部分人份的票，
     * 只作废本人持有的人份，别人已领取的人份不动），随即删除不再有可用入场权益的人脸绑定；
     * 作废本人持有的各人份上的在途转赠（BSM 第 809 行「接收人确认领取后可以继续……再次转赠」——别人已领取的人份及其再转赠不动）。返回作废的权益数。
     */
    public int disposeHeldTicketsOnDeregistration(String customerId, java.time.Instant now) {
        int forfeited = 0;
        java.util.Set<String> parents = new java.util.LinkedHashSet<>();
        for (IssuedEntitlementRecord e : repository.listHeldEntitlements(customerId)) {
            if (e.parentTicketId() != null) {
                parents.add(e.parentTicketId());
            }
            if ("active".equals(e.status()) && e.usedTimes() < e.totalTimes()) {
                forfeited += repository.forfeitHeldEntitlement(e.id(), now);
            }
        }
        if (transferService != null) {
            for (TicketSlotHolderRecord holder : repository.listSlotHoldersByHolder(customerId)) {
                transferService.voidPendingTransfersForSlot(holder.parentTicketId(), holder.slotIndex(), "account_deregistered", now);
            }
        }
        if (facePhotos != null) {
            for (String parentId : parents) {
                facePhotos.deleteIfNoUsableEntry(parentId, now);
            }
        }
        return forfeited;
    }

    /** ROOT-03-R21 注销删除个人信息：清空本人各订单的购票手机号及其盲索引。 */
    public int clearBuyerPhonesOnDeregistration(String customerId) {
        return repository.clearBuyerPhonesOfCustomer(customerId);
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

    /** TK-05-R2/R3 登记通票安全事件（BSM:804 通票安全例外；须记录原因、当时情形），待夜班值班经理及以上批准。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.request")
    @Transactional
    public TicketSafetyEventRecord requestTicketSafetyEvent(com.complex.platform.ticketing.dto.TicketSafetyEventRequest request) {
        return operations.executeWrite(
                "requestTicketSafetyEvent",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketSafetyEventRecord.class,
                TicketingWriteOperation.REQUEST_TICKET_SAFETY_EVENT,
                () -> safetyExceptionService.request(request)
        );
    }

    /** TK-05-R2/R3 批准通票安全事件：非多次通票退该人份票款（已核销也退），多次通票恢复本次 1 次完整补偿使用机会。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.approve")
    @Transactional
    public TicketSafetyEventRecord approveTicketSafetyEvent(String eventId, StatusChangeRequest request) {
        String id = operations.requireText(eventId, "safety_event_id");
        return operations.executeWrite(
                "approveTicketSafetyEvent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketSafetyEventRecord.class,
                TicketingWriteOperation.APPROVE_TICKET_SAFETY_EVENT,
                () -> safetyExceptionService.approve(id, request)
        );
    }

    /** TK-05 通票安全事件台账（待批准在前）；权限 ticket.refund.view。 */
    public PageResponse<TicketSafetyEventRecord> listTicketSafetyEvents() {
        return operations.readPage(TicketingReadOperation.LIST_TICKET_SAFETY_EVENTS, () -> safetyExceptionService.list());
    }

    /**
     * 退款单台账（后台“待确认拨付”看板）：业务人员靠它看哪几笔已批准但钱还没打出去。
     * 权限 ticket.refund.view；商户 scope 只见本商户受理的退款（V220 退款单直挂 merchant_id）；不挂商户的项目员工只见原单
     * 在按该权限码可触及项目内的退款（第 4 条，PGM:659「所有业务数据默认按项目隔离，普通岗位没有跨项目查看权」；原单取不到项目的不给）；
     * 平台管理员与最高权限不限（同安全事件台账）。
     */
    public PageResponse<TicketRefundRecord> listTicketRefunds() {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_REFUNDS,
                () -> {
                    List<TicketRefundRecord> rows = filterByMerchantScope(repository.listTicketRefunds(), TicketRefundRecord::merchantId);
                    if (authorizationService == null || authorizationService.currentMerchantIdQuietly().isPresent()) {
                        return rows;
                    }
                    var actor = authorizationService.currentActor();
                    if (authorizationService.isMasterAuthority(actor)) {
                        return rows;
                    }
                    var reach = authorizationService.projectReach(actor, "ticket.refund.view");
                    if (reach.platformAdmin()) {
                        return rows;
                    }
                    java.util.Map<String, Boolean> visible = new java.util.HashMap<>();
                    return rows.stream().filter(refund -> visible.computeIfAbsent(refund.targetId(), orderId ->
                            reach.covers(repository.findTicketOrderById(orderId).map(TicketOrderRecord::projectId).orElse(null))))
                            .toList();
                }
        );
    }

    /** 财务实际打款后确认拨付（approved→success），权限沿用 ticket.refund.approve。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.approve")
    @Transactional
    public TicketRefundRecord confirmTicketRefundPayout(String refundId, StatusChangeRequest request) {
        String id = operations.requireText(refundId, "refund_id");
        return operations.executeWrite(
                "confirmTicketRefundPayout:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.CONFIRM_TICKET_REFUND_PAYOUT,
                () -> refundService.confirmPayout(id, request),
                payoutResultMetadata(request)
        );
    }

    /**
     * 第十八批 AH 段第 256 条 256-R1（BSM:806「系统完整记录……原路退回结果」；PGM:899「结果是什么，都要能查到」）：
     * 确认拨付的成功留痕记原路退回结果。执行前组好；为空时服务层按必填拒，这里给空元数据（Map.of 不收 null）。
     */
    private java.util.Map<String, Object> payoutResultMetadata(StatusChangeRequest request) {
        String result = request == null ? null : com.complex.platform.common.Strings.trimToNull(request.reason());
        return result == null ? java.util.Map.of() : java.util.Map.of("payout_result", result);
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

    @Transactional
    public VerificationPreviewResponse previewVerification(VerificationRequest request) {
        verificationService.requireAmusementRequestScope(request);
        VerificationPreviewResponse result = operations.executeWrite(
                "previewTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationPreviewResponse.class,
                TicketingWriteOperation.PREVIEW_VERIFICATION,
                () -> verificationService.previewVerification(request)
        );
        verificationService.requireAmusementRequestScope(request);
        return result;
    }

    @com.complex.platform.gate.GuardedAction(capability = "verification.execute")
    @Transactional
    public VerificationResponse createVerification(VerificationRequest request) {
        verificationService.requireAmusementRequestScope(request);
        if (locationGuard != null) {
            locationGuard.check("createTicketVerification", () -> verificationService.requestProjectId(request));
        }
        VerificationResponse result = operations.executeWrite(
                "createTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                TicketingWriteOperation.CREATE_VERIFICATION,
                () -> verificationService.createVerification(request)
        );
        verificationService.requireAmusementRequestScope(request);
        return result;
    }

    /** R5 T5：核销冲正。幂等键必填、reason 必填、权限 ticket.verify.reverse；成功事件 ticket.verification_reversed。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.verify.reverse", admissionAction = "ticketing.verification_reverse", objectType = "verification")
    @Transactional
    public VerificationResponse reverseVerification(String verificationId, VerificationReverseRequest request, String reversedBy) {
        verificationService.requireAmusementReversalScope(verificationId);
        VerificationResponse result = operations.executeWrite(
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
        verificationService.requireAmusementReversalScope(verificationId);
        return result;
    }

    /** TK-14：已成功扣次的核销提起异常复核（保留原成功记录与扣次）；权限 verification.execute，幂等+审计。 */
    @com.complex.platform.gate.GuardedAction(capability = "verification.execute")
    @Transactional
    public VerificationRecord raiseVerificationAnomaly(String verificationId, VerificationReverseRequest request, String raisedBy) {
        return operations.executeWrite(
                "raiseTicketVerificationAnomaly",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationRecord.class,
                TicketingWriteOperation.RAISE_VERIFICATION_ANOMALY,
                () -> verificationService.raiseVerificationAnomaly(verificationId, request == null ? null : request.reason(), raisedBy)
        );
    }

    /** TK-14：主管二选一处置核销异常（restored / voided_reversed），原因必填；权限 ticket.verify.anomaly.resolve，幂等+审计。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.verify.anomaly.resolve")
    @Transactional
    public VerificationRecord resolveVerificationAnomaly(String anomalyId, VerificationAnomalyResolveRequest request, String resolvedBy) {
        // 作废并冲正先过与直接冲正相同的合同准入闸（对原核销记录）；更正后恢复不动次数，不过闸。
        if (request != null && "voided_reversed".equals(request.resolution()) && anomalyId != null) {
            repository.findVerificationById(anomalyId).map(VerificationRecord::anomalyOfVerificationId)
                    .ifPresent(original -> verificationService.admitAnomalyVoidReversal(original, request.reason()));
        }
        return operations.executeWrite(
                "resolveTicketVerificationAnomaly",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationRecord.class,
                TicketingWriteOperation.RESOLVE_VERIFICATION_ANOMALY,
                () -> verificationService.resolveVerificationAnomaly(anomalyId, request == null ? null : request.resolution(),
                        request == null ? null : request.reason(), resolvedBy)
        );
    }

    public PageResponse<VerificationRecord> listVerifications() {
        return operations.readPage(
                TicketingReadOperation.LIST_VERIFICATIONS,
                () -> filterByMerchantScope(repository.listVerifications(), VerificationRecord::merchantId)
        );
    }
}
