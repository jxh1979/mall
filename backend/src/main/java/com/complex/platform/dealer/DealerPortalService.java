package com.complex.platform.dealer;

// 文件版本：V0.05

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPortalProfileRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.TerminalIdentityHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * CG-11d 独立经销商 Web 门户（受控第十一端 dealer_portal，ENUM:233；BSM:1217-1218）。
 * <ul>
 *   <li>隔离账号：只有 account_type = dealer 且归属 dealer_portal_accounts 的账号可进；内部员工 / 商户账号一律拒绝并留痕。</li>
 *   <li>隔离终端：请求终端必须是 dealer_portal（员工 Web 端、商户门户自报的终端不得进），否则按访问拒绝留痕 terminal.access_rejected。</li>
 *   <li>隔离数据：所有读写只落本经销商；别家对象按不存在处理。</li>
 *   <li>门户资格：企业资料、合同、开票、银行资料审核通过（portal_enabled）前只开放资料提交；停用后立即拒绝（ENUM:181）。</li>
 *   <li>取消申请与查看发票（ENUM:179、裁决 69）：取消只是申请（→ 取消中），仍由内部按既有审批及订单、付款互斥规则办理，门户无审批入口；发票只读。</li>
 *   <li>统一终端开关、兜底关闭由 TerminalMasterSwitchInterceptor / TerminalSwitchGuard 对 dealer_portal 逐请求强制（V454 总开关行）。</li>
 * </ul>
 */
@Service
public class DealerPortalService {

    static final String TERMINAL = "dealer_portal";
    static final String PROFILE_VIEW = "dealer_portal.profile.view";
    static final String PROFILE_SUBMIT = "dealer_portal.profile.submit";
    static final String STOCK_VIEW = "dealer_portal.stock.view";
    static final String INQUIRY_MANAGE = "dealer_portal.inquiry.manage";
    static final String ORDER_MANAGE = "dealer_portal.order.manage";
    static final String ORDER_PAY = "dealer_portal.order.pay";
    static final String RETURN_REQUEST = "dealer_portal.return.request";
    private static final String REJECT_EVENT = "dealer.portal.access_rejected";

    private final JdbcDealerPortalRepository portalAccounts;
    private final JdbcDealerRepository dealers;
    private final JdbcDealerOrderRepository orderRepository;
    private final DealerService dealerService;
    private final DealerOrderService orderService;
    private final DealerReturnService returnService;
    private final DealerReturnFinancialService returnFinancial;
    private final AuthorizationService authorization;
    private final AuditEventService audit;
    private final DealerDeliveryService delivery;
    private final com.complex.platform.evidence.EvidenceEventService attachmentService;
    private final com.complex.platform.evidence.EvidenceEventRepository attachmentRepository;

    public DealerPortalService(JdbcDealerPortalRepository portalAccounts, JdbcDealerRepository dealers, JdbcDealerOrderRepository orderRepository,
            DealerService dealerService, DealerOrderService orderService, DealerReturnService returnService, AuthorizationService authorization,
            AuditEventService audit, DealerDeliveryService delivery,
            com.complex.platform.evidence.EvidenceEventService attachmentService, com.complex.platform.evidence.EvidenceEventRepository attachmentRepository, DealerReturnFinancialService returnFinancial) {
        this.portalAccounts = portalAccounts;
        this.dealers = dealers;
        this.orderRepository = orderRepository;
        this.dealerService = dealerService;
        this.orderService = orderService;
        this.returnService = returnService;this.returnFinancial=returnFinancial;
        this.authorization = authorization;
        this.audit = audit;
        this.delivery = delivery;
        this.attachmentService = attachmentService;
        this.attachmentRepository = attachmentRepository;
    }

    // ── 资料 ──

    public DealerRecord me() {
        DealerRecord dealer = currentDealer(false);
        authorization.requirePermission(PROFILE_VIEW);
        return dealer;
    }

    /** 注册资料提交：待提交 / 已驳回 → 审核中（BSM:1217）。 */
    @Transactional
    public DealerRecord submitProfile(DealerPortalProfileRequest request) {
        DealerRecord dealer = currentDealer(false);
        authorization.requirePermission(PROFILE_SUBMIT);
        return dealerService.submitProfile(dealer.id(), request, PROFILE_SUBMIT);
    }

    public PageResponse<DealerContractRecord> listContracts() {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(PROFILE_VIEW);
        return dealerService.listContracts(dealer.id(), PROFILE_VIEW);
    }

    /** 库存外显：只显示有货 / 紧张 / 缺货 / 预计交期（BSM:1221）。 */
    public PageResponse<DealerExternalStockRecord> externalStock(String contractId) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(STOCK_VIEW);
        requireOwnContract(dealer, contractId);
        return dealerService.externalStock(contractId, STOCK_VIEW);
    }

    // ── 询价 ──

    public PageResponse<DealerInquiryRecord> listInquiries() {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(INQUIRY_MANAGE);
        return dealerService.listInquiries(dealer.id(), INQUIRY_MANAGE);
    }

    @Transactional
    public DealerInquiryRecord createInquiry(DealerInquiryRequest request) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(INQUIRY_MANAGE);
        requireOwnContract(dealer, request == null ? null : request.dealerContractId());
        return dealerService.createInquiry(dealer.id(), request, INQUIRY_MANAGE);
    }

    // ── 订单 ──

    public PageResponse<DealerOrderRecord> listOrders() {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        return orderService.listOrders(dealer.id(), ORDER_MANAGE);
    }

    public DealerOrderRecord getOrder(String orderId) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        return orderService.getOrder(requireOwnOrder(dealer, orderId).id(), ORDER_MANAGE);
    }

    public PageResponse<DealerOrderLineRecord> listLines(String orderId) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        return orderService.listLines(requireOwnOrder(dealer, orderId).id(), ORDER_MANAGE);
    }

    @Transactional
    public DealerOrderRecord createOrder(DealerOrderDraftRequest request) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        requireOwnContract(dealer, request == null ? null : request.dealerContractId());
        return orderService.createOrderDraft(dealer.id(), request, ORDER_MANAGE);
    }

    @Transactional
    public DealerOrderRecord submitOrder(String orderId, String idempotencyKey) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        return orderService.submitOrder(requireOwnOrder(dealer, orderId).id(), idempotencyKey, ORDER_MANAGE);
    }

    public PageResponse<DealerOrderPaymentRecord> listPayments(String orderId) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_PAY);
        return orderService.listPayments(requireOwnOrder(dealer, orderId).id(), ORDER_PAY);
    }

    /** 付款登记：只接受对公转账 / 平台线上支付（BSM:1230），由财务核验到账。 */
    @Transactional
    public DealerOrderPaymentRecord registerPayment(String orderId, DealerPaymentRequest request) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_PAY);
        return orderService.registerPayment(requireOwnOrder(dealer, orderId).id(), request, ORDER_PAY);
    }

    @Transactional
    public DealerOrderRecord signOrder(String orderId, com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest request) {
        String ownedOrderId;
        try {
            DealerRecord dealer = currentDealer(true);
            authorization.requirePermission(ORDER_MANAGE);
            ownedOrderId = requireOwnOrder(dealer, orderId).id();
        } catch (RuntimeException error) {
            recordDeliveryFailure("dealer.delivery.receive_failed","dealer_order",orderId,request==null?null:request.idempotencyKey(),error);
            throw error;
        }
        return delivery.receive(ownedOrderId, request, ORDER_MANAGE);
    }

    public DealerDeliveryRecord delivery(String orderId) {
        String ownedOrderId;
        try {
            DealerRecord dealer = currentDealer(true);
            authorization.requirePermission(ORDER_MANAGE);
            ownedOrderId = requireOwnOrder(dealer, orderId).id();
        } catch (RuntimeException error) {
            recordDeliveryFailure("dealer.delivery.view_failed","dealer_order",orderId,null,error);
            throw error;
        }
        return delivery.get(ownedOrderId, ORDER_MANAGE);
    }

    public com.complex.platform.evidence.EvidenceAttachmentRecord uploadDeliveryEvidence(String orderId, org.springframework.web.multipart.MultipartFile file) {
        try {
            DealerRecord dealer = currentDealer(true);
            authorization.requirePermission(ORDER_MANAGE);
            requireOwnOrder(dealer, orderId);
        } catch (RuntimeException error) {
            recordDeliveryFailure("evidence.attachment_upload_failed","dealer_delivery",orderId,null,error);
            throw error;
        }
        var attachment = attachmentService.uploadAttachment(file, "dealer_delivery", orderId, "other");
        return new com.complex.platform.evidence.EvidenceAttachmentRecord(attachment.id(), attachment.fileName(), attachment.contentType(), attachment.fileSize(),
                attachment.businessType(), attachment.businessId(), attachment.attachmentRole(), attachment.objectKey(),
                "/api/v1/dealer-portal/orders/" + orderId + "/delivery-evidence/" + attachment.id(), attachment.status(), attachment.checksum(),
                attachment.createdAt(), attachment.updatedAt(), attachment.merchantId());
    }

    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> readDeliveryEvidence(String orderId, String attachmentId) {
        try {
            DealerRecord dealer = currentDealer(true);
            authorization.requirePermission(ORDER_MANAGE);
            requireOwnOrder(dealer, orderId);
            var attachment = attachmentRepository.findAttachmentById(attachmentId)
                    .filter(a -> "dealer_delivery".equals(a.businessType()) && orderId.equals(a.businessId()))
                    .orElseThrow(() -> ApiException.notFound("ATTACHMENT_NOT_FOUND", "本订单交付凭证不存在"));
            java.nio.file.Path path = attachmentService.resolveAttachmentFile(attachmentId);
            var response = org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(attachment.contentType()))
                    .body((org.springframework.core.io.Resource)new org.springframework.core.io.FileSystemResource(path));
            audit.recordSuccess("dealer.portal.delivery_evidence_viewed", ORDER_MANAGE, "evidence_attachment", attachmentId, null, null, Map.of("order_id",orderId));
            return response;
        } catch (RuntimeException error) {
            recordDeliveryFailure("dealer.portal.delivery_evidence_view_failed","evidence_attachment",attachmentId,null,error);
            throw error;
        }
    }

    private void recordDeliveryFailure(String event,String type,String id,String key,RuntimeException error) {
        try { audit.recordFailure(event,ORDER_MANAGE,type,id,null,key,error instanceof ApiException api?api.code():error.getClass().getSimpleName()); }
        catch(RuntimeException auditError) { error.addSuppressed(auditError); }
    }

    // ── 退货 ──

    public PageResponse<DealerReturnRecord> listReturns(String orderId) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(RETURN_REQUEST);
        return returnService.listReturns(requireOwnOrder(dealer, orderId).id(), RETURN_REQUEST);
    }

    @Transactional
    public DealerReturnRecord requestReturn(String orderId, DealerReturnRequest request) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(RETURN_REQUEST);
        return returnService.requestReturn(requireOwnOrder(dealer, orderId).id(), request, RETURN_REQUEST);
    }

    public java.util.List<JdbcDealerReturnRepository.ReturnSource> returnSources(String orderId) {
        return readOrderData(orderId,RETURN_REQUEST,"dealer.portal.return_sources_viewed","dealer.portal.return_sources_view_failed",()->returnFinancial.ownedReturnSources(orderId));
    }
    public java.util.List<DealerReturnFinancialRecord> returnFinancials(String orderId) {
        return readOrderData(orderId,RETURN_REQUEST,"dealer.portal.return_financials_viewed","dealer.portal.return_financials_view_failed",()->returnFinancial.ownedReturnFinancials(orderId));
    }

    // ── 取消申请 / 发票（ENUM:179、裁决 69）──

    /** 「门户提供……查看发票」：只读本企业订单的发票事实（金额 / 状态 / 发票号 / 重开自）；开票、红冲、重开仍由内部办理。 */
    public PageResponse<DealerInvoiceRecord> listInvoices(String orderId) {
        return readOrderData(orderId,ORDER_MANAGE,"dealer.portal.invoices_viewed","dealer.portal.invoices_view_failed",()->returnService.listInvoices(orderId,ORDER_MANAGE));
    }

    /** 「取消只是申请，仍由内部经办按既有审批及订单、付款互斥规则办理，不允许经销商自批取消」：复用内部取消申请（→ 取消中），审批只在内部后台（dealer.order.review）。 */
    @Transactional
    public DealerOrderRecord requestCancel(String orderId, DealerCancelRequest request) {
        DealerRecord dealer = currentDealer(true);
        authorization.requirePermission(ORDER_MANAGE);
        return orderService.requestCancel(requireOwnOrder(dealer, orderId).id(), request, ORDER_MANAGE);
    }

    private <T>T readOrderData(String orderId,String permission,String event,String failure,java.util.function.Supplier<T> query) {
        try {
            DealerRecord dealer=currentDealer(true);authorization.requirePermission(permission);requireOwnOrder(dealer,orderId);
            T result=query.get();audit.recordSuccess(event,permission,"dealer_order",orderId,null,null,Map.of());return result;
        } catch(RuntimeException error) {
            try {audit.recordFailure(failure,permission,"dealer_order",orderId,null,null,error instanceof ApiException api?api.code():error.getClass().getSimpleName());}
            catch(RuntimeException auditError){error.addSuppressed(auditError);}throw error;
        }
    }

    // ── 隔离闸 ──

    /**
     * 解析当前门户经销商：账号类型 → 终端 → 归属 → 停用 → 门户资格。
     * @param requirePortalEnabled 所有门户能力均须四项审核通过；保留调用签名不放宽资料页。
     */
    DealerRecord currentDealer(boolean requirePortalEnabled) {
        AccessActor actor = authorization.currentActor();
        if (!DealerPortalAccountService.ACCOUNT_TYPE.equals(actor.accountType())) {
            rejectAccount(actor, "account_type_not_dealer");
            throw ApiException.forbidden("DEALER_PORTAL_ACCOUNT_REQUIRED", "经销商门户只接受经销商门户账号登录");
        }
        String terminal = currentTerminal();
        if (!TERMINAL.equals(terminal)) {
            rejectTerminal(actor, terminal);
            throw ApiException.forbidden("TERMINAL_NOT_ALLOWED", "经销商门户只能从独立经销商门户终端访问");
        }
        DealerPortalAccountRecord account = portalAccounts.findByUser(actor.id()).orElse(null);
        if (account == null) {
            rejectAccount(actor, "portal_account_missing");
            throw ApiException.forbidden("DEALER_PORTAL_ACCOUNT_REQUIRED", "该账号未归属任何经销商");
        }
        DealerRecord dealer = dealers.findDealer(account.dealerId()).orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        if (DealerService.DEACTIVATED.equals(dealer.status()) || "revoked".equals(account.status())) {
            rejectAccount(actor, "dealer_deactivated");
            throw ApiException.forbidden("DEALER_DEACTIVATED", "经销商已停用，门户及接口权限已撤销");
        }
        if (!"active".equals(account.status()) || !DealerService.APPROVED.equals(dealer.status()) || !dealer.portalEnabled()) {
            throw ApiException.forbidden("DEALER_PORTAL_NOT_ENABLED", "企业资料、合同、开票和银行资料审核通过后才启用门户功能");
        }
        return dealer;
    }

    private void requireOwnContract(DealerRecord dealer, String contractId) {
        DealerContractRecord contract = dealers.findContract(requireText(contractId, "dealer_contract_id")).orElse(null);
        if (contract == null || !contract.dealerId().equals(dealer.id())) {
            throw ApiException.notFound("DEALER_CONTRACT_NOT_FOUND", "合同不存在");
        }
    }

    private DealerOrderRecord requireOwnOrder(DealerRecord dealer, String orderId) {
        DealerOrderRecord order = orderRepository.findOrder(requireText(orderId, "order_id")).orElse(null);
        if (order == null || !order.dealerId().equals(dealer.id())) {
            throw ApiException.notFound("DEALER_ORDER_NOT_FOUND", "经销订单不存在");
        }
        return order;
    }

    private static String currentTerminal() {
        String header = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            header = request.getHeader("X-Terminal-Type");
        }
        String terminal = TerminalIdentityHolder.trustedTerminalType(header);
        return terminal == null ? "" : terminal.trim();
    }

    private void rejectAccount(AccessActor actor, String reason) {
        try {
            audit.recordSuccessIndependently(REJECT_EVENT, PROFILE_VIEW, "access_user", actor.id(), null, null,
                    meta("reason", reason, "account_type", actor.accountType(), "terminal_type", currentTerminal(), "result", "failure"));
        } catch (RuntimeException ignored) {
            // 留痕失败不得掩盖拒绝结果
        }
    }

    /** 与 TerminalSwitchGuard 同一事件名，终端开关矩阵与访问留痕一处可查。 */
    private void rejectTerminal(AccessActor actor, String terminal) {
        try {
            audit.recordSuccessIndependently("terminal.access_rejected", "terminal.switch.manage", "terminal_capability_switch", "dealer_portal.access", null, null,
                    meta("terminal_type", terminal, "capability", "dealer_portal.access", "reason", "terminal_mismatch", "user_id", actor.id(), "result", "failure"));
        } catch (RuntimeException ignored) {
            // 留痕失败不得掩盖拒绝结果
        }
    }

    private static Map<String, Object> meta(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }
}
